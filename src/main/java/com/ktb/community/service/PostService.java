package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequestDto;
import com.ktb.community.dto.response.PostResponseDto;
import com.ktb.community.entity.Image;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostImage;
import com.ktb.community.entity.User;
import com.ktb.community.repository.ImageRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ImageRepository imageRepository;


    @Value("${aws.cloud_front.domain}")
    private String cloudfrontDomain;
    // 게시글 작성
    public Long createPost(PostCreateRequestDto dto, Long userId) {

        //1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. post 엔티티 생성 및 기본 정보 설정
        Post post = Post.builder()
                .title(dto.getTitle())
                .contents(dto.getContent())
                .build();
        post.setUser(user);

        // 3. 이미지 정보 처리
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (PostCreateRequestDto.ImageInfo imageInfo : dto.getImages()) {
                // 3-1. Image 엔티티 생성
                Image image = Image.builder()
                        .s3_key(imageInfo.getS3_key())
                        .user(user)
                        .build();

                // 3-2. PostImage 엔티티(매핑 테이블) 생성
                PostImage postImage = PostImage.builder()
                        .post(post)
                        .image(image)
                        .orders(imageInfo.getOrder())
                        .build();
                post.setPostImageList(postImage); // 연관관계 설정
            }
        }

        // 4. post 엔티티 저장 ( cascade 설정으로 postimage, image, postCount 함께 저장)
        Post savedPost = postRepository.save(post);

        return savedPost.getId();
    }

    // 게시글 단건 조회(상세 페이지)
    public PostResponseDto getPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();

        // 1. 엔티티의 이미지 목록을 dto로 변환
        List<PostResponseDto.ImageInfo> imageInfos = post.getPostImageList().stream()
                .map(postImage -> {
                    Image image = postImage.getImage();
                    // 2. S3 Key에 CloudFront 도메인을 붙여 완전한 URL 생성
                    String imageUrl = cloudfrontDomain + "/" + image.getS3_key();
                    return new PostResponseDto.ImageInfo(imageUrl, postImage.getOrders());
                })
                .collect(Collectors.toList());

        // 3. 최종 응답 dto 반환
        return new PostResponseDto(
                post.getId(),
                post.getTitle(),
                post.getContents(),
                post.getUser().getNickname(),
                post.getUpdatedAt(),
                imageInfos
        );
    }

    // 게시글 전체 조회(인피니티 스크롤)

    // 게시글 수정
    public void updatePost(Long postId, PostCreateRequestDto requestDto, Long userId) {
        // 1. 게시물 조회 및 수정 권한 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("게시물을 수정할 권한이 없습니다.");
        }

        // 2. 사용하지 않을 S3 파일 삭제
        deleteS3Image(requestDto, post);

        // 3. 제목 및 내용 업데이트
        post.update(requestDto.getTitle(), requestDto.getContent()); // Post 엔티티에 업데이트 메서드 추천

        // 4. 이미지 목록 업데이트 (기존 목록을 모두 지우고 새로 추가하는 방식)
        // orphanRemoval = true 옵션 덕분에 postImageList에서 제거된 PostImage는 DB에서도 삭제됨
        post.getPostImageList().clear();

        if (requestDto.getImages() != null && !requestDto.getImages().isEmpty()) {
            for (PostCreateRequestDto.ImageInfo imageInfo : requestDto.getImages()) {
                // s3_key로 Image 엔티티를 찾거나, 없다면 새로 생성 (
                Image image = imageRepository.findByS3Key(imageInfo.getS3_key())
                        .orElseGet(() -> imageRepository.save(new Image(imageInfo.getS3_key(), post.getUser())));

                PostImage postImage = new PostImage(post, image, imageInfo.getOrder());
                post.addPostImage(postImage); // Post 엔티티에 연관관계 편의 메서드 추천
            }
        }
    }

    // 게시글 삭제
    public void deletePost(Long postId, Long userId) throws AccessDeniedException {
        // 1. 게시물 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물을 찾을 수 없습니다."));

        // 2. 소유권 확인 (게시물 작성자 ID와 현재 사용자 ID 비교)
        if (!post.getUser().getId().equals(userId)) {
            // Admin 권한이 있다면 이 로직을 통과시키는 로직을 추가할 수도 있습니다.
            throw new AccessDeniedException("게시물을 삭제할 권한이 없습니다.");
        }

        // 3. s3에서 이미지 삭제
        if (post.getPostImageList() != null && !post.getPostImageList().isEmpty()) {
            for (PostImage postImage : post.getPostImageList()) {
                s3Service.deleteFile(postImage.getImage().getS3_key());
            }
        }

        // 4. 게시물 삭제
        // post 엔티티의 cascade 설정으로 인해 연관된 postImage, Comment, PostCount가 함께 삭제
        postRepository.delete(post);
    }


    // 이미지 update시 사용하지 않는 s3 이미지 삭제
    private void deleteS3Image(PostCreateRequestDto requestDto, Post post) {
        // 1. 기존에 저장되어 있던 이미지의 S3 Key 목록을 추출합니다.
        Set<String> existingImageKeys = post.getPostImageList().stream()
                .map(postImage -> postImage.getImage().getS3_key())
                .collect(Collectors.toSet());

        // 2. 요청 DTO에 포함된 새로운 이미지의 S3 Key 목록을 추출합니다.
        Set<String> newImageKeys = (requestDto.getImages() != null)
                ? requestDto.getImages().stream()
                .map(PostCreateRequestDto.ImageInfo::getS3_key)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        // 3. 기존 Key 목록(existingImageKeys)에서 새로운 Key 목록(newImageKeys)을 뺀다.
        // -> 결과적으로 삭제되어야 할 이미지의 Key 목록이 남습니다.
        existingImageKeys.removeAll(newImageKeys);

        // 4. 삭제해야 할 Key 목록을 순회하며 S3에서 실제 파일을 삭제합니다.
        for (String keyToDelete : existingImageKeys) {
            s3Service.deleteFile(keyToDelete);
        }
    }
}
