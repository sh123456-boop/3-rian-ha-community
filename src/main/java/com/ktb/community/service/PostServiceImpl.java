package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequestDto;
import com.ktb.community.dto.response.PostTop5ResponseDto;
import com.ktb.community.dto.response.PostResponseDto;
import com.ktb.community.dto.response.PostSliceResponseDto;
import com.ktb.community.dto.response.PostSummaryDto;
import com.ktb.community.entity.*;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.ImageRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.UserLikePostsRepository;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ktb.community.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService{

    private final PostRepository postRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final UserRepository userRepository;
    private final S3ServiceImpl s3Service;
    private final ImageRepository imageRepository;
    private final UserLikePostsRepository userLikePostsRepository; // 좋아요 레포지토리 주입
    private static final int PAGE_SIZE = 10; // 한 페이지에 보여줄 게시물 수



    @Value("${aws.cloud_front.domain}")
    private String cloudfrontDomain;

    @Value("${aws.cloud_front.default-profile-image-key}")
    private String defaultProfileImageKey;

    // 게시글 작성
    @Transactional
    public Long createPost(PostCreateRequestDto dto, Long userId) {

        //1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(MEMBER_NOT_FOUND));

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
                        .s3Key(imageInfo.getS3_key())
                        .user(null)
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
    @Transactional
    public PostResponseDto getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(POST_NOT_FOUND));

        // 조회수 증가 로직 호출
        increaseViewCount(post);

        // 레디스 자료구조에 조회수 + 1(일일)
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        String dailyKey = getDailyRankingKey();
        String weeklyKey = getWeeklyRankingKey();

        zSetOperations.incrementScore(dailyKey, postId.toString(), 1);
        zSetOperations.incrementScore(weeklyKey, postId.toString(), 1);

        if (redisTemplate.getExpire(dailyKey) < 0) {
            // 일일 키에 2일의 유효시간 설정
            redisTemplate.expire(dailyKey, 2, TimeUnit.DAYS);
        }
        if (redisTemplate.getExpire(weeklyKey) < 0) {
            // 주간 키에 8일의 유효시간 설정
            redisTemplate.expire(weeklyKey, 8, TimeUnit.DAYS);
        }

        // 작성자의 프로필 이미지 URL을 생성
        String authorProfileImageUrl;
        User author = post.getUser();

        if (author != null && author.getImage() != null) {
            // 유저의 프로필 이미지가 있으면 -> 해당 이미지의 URL 생성
            String s3Key = author.getImage().getS3Key();
            authorProfileImageUrl = "https://" + cloudfrontDomain + "/" + s3Key;
        } else {
            // 유저의 프로필 이미지가 없으면 -> 설정해둔 기본 이미지 URL 사용
            authorProfileImageUrl = "https://" + cloudfrontDomain + "/" + defaultProfileImageKey;
        }


        // 1. 엔티티의 이미지 목록을 dto로 변환
        List<PostResponseDto.ImageInfo> imageInfos = post.getPostImageList().stream()
                .map(postImage -> {
                    Image image = postImage.getImage();
                    // 2. S3 Key에 CloudFront 도메인을 붙여 완전한 URL 생성
                    String imageUrl = "https://" + cloudfrontDomain + "/" + image.getS3Key();
                    return new PostResponseDto.ImageInfo(imageUrl, postImage.getOrders(), image.getS3Key());
                })
                .collect(Collectors.toList());

        // 좋아요 누른 게시글인지 확인
        boolean likedByUser = userLikePostsRepository.existsByUserAndPost(author, post);




        // 3. 최종 응답 dto 반환
        return new PostResponseDto(post, imageInfos, authorProfileImageUrl, author.getId(), likedByUser);
    }

    // 게시글 전체 조회(인피니티 스크롤)
    @Transactional(readOnly = true)
    public PostSliceResponseDto getPostSlice(Long lastPostId) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        // 1. Fetch Join이 적용된 새로운 Repository 메서드를 호출
        Slice<Post> postSlice = (lastPostId == null)
                ? postRepository.findSliceByOrderByIdDesc(pageable)
                : postRepository.findSliceByIdLessThanOrderByIdDesc(lastPostId, pageable);

        // 2. 스트림 내에서 DTO를 변환하며 프로필 이미지 URL을 생성
        List<PostSummaryDto> posts = getPostSummaryDtos(postSlice.getContent());

        return new PostSliceResponseDto(posts, postSlice.hasNext());
    }

    // 게시글 인기순 전체 조회(인피니티 스크롤)
    @Transactional(readOnly = true)
    public PostSliceResponseDto getPopularPostSlice(Long lastViewCount, Long lastPostId) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        // 1. Fetch Join이 적용된 새로운 Repository 메서드를 호출
        Slice<Post> postSlice = (lastPostId == null)
                ? postRepository.findSliceByOrderByViewCountDesc(pageable)
                : postRepository.findSliceByOrderByViewCountDesc(lastViewCount, lastPostId, pageable);

        // 2. 스트림 내에서 DTO를 변환하며 프로필 이미지 URL을 생성
        List<PostSummaryDto> posts = getPostSummaryDtos(postSlice.getContent());

        return new PostSliceResponseDto(posts, postSlice.hasNext());
    }



    // 게시글 수정
    @Transactional
    public void updatePost(Long postId, PostCreateRequestDto requestDto, Long userId) {
        // 1. 게시물 조회 및 수정 권한 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED);
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
                        .orElseGet(() -> imageRepository.save(new Image(imageInfo.getS3_key(), null)));

                PostImage postImage = new PostImage(post, image, imageInfo.getOrder());
                post.addPostImage(postImage); // Post 엔티티에 연관관계 편의 메서드 추천
            }
        }
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, Long userId) throws AccessDeniedException {
        // 1. 게시물 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(POST_NOT_FOUND));

        // 2. 소유권 확인 (게시물 작성자 ID와 현재 사용자 ID 비교)
        if (!post.getUser().getId().equals(userId)) {
            // Admin 권한이 있다면 이 로직을 통과시키는 로직을 추가할 수도 있습니다.
            throw new BusinessException(ACCESS_DENIED);
        }

        // 3. s3에서 이미지 삭제
        if (post.getPostImageList() != null && !post.getPostImageList().isEmpty()) {
            for (PostImage postImage : post.getPostImageList()) {
                s3Service.deleteFile(postImage.getImage().getS3Key());
            }
        }

        // 4. 게시물 삭제
        post.getUser().getPostList().remove(post);
        // post 엔티티의 cascade 설정으로 인해 연관된 postImage, Comment, PostCount가 함께 삭제
        postRepository.delete(post);
    }


    // 이미지 update시 사용하지 않는 s3 이미지 삭제
    @Transactional
    private void deleteS3Image(PostCreateRequestDto requestDto, Post post) {
        // 1. 기존에 저장되어 있던 이미지의 S3 Key 목록을 추출합니다.
        Set<String> existingImageKeys = post.getPostImageList().stream()
                .map(postImage -> postImage.getImage().getS3Key())
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

    // 게시글 좋아요 추가 메서드
    @Transactional
    public void likePost(Long postId, Long userId) {
        // 1. 게시물과 사용자 정보를 조회합니다.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        // 2. 이미 해당 게시물에 좋아요를 눌렀는지 확인합니다.
        if (userLikePostsRepository.existsByUserAndPost(user, post)) {
            throw new BusinessException(LIKE_DUPLICATION);
        }

        // 3. 좋아요 기록을 생성하고 저장합니다 (UserLikePosts).
        UserLikePosts userLikePosts = new UserLikePosts(user, post);
        userLikePostsRepository.save(userLikePosts);

        // 4. 게시물의 좋아요 수를 1 증가시킵니다 (PostCount).
        post.getPostCount().increaseLikesCount();
    }

    // 게시글 좋아요 취소 메서드
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        // 1. 게시물과 사용자 정보를 조회합니다.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        // 2. 사용자가 해당 게시물에 좋아요를 누른 기록을 조회합니다.
        UserLikePosts userLikePosts = userLikePostsRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new BusinessException(UNLIKE_DUPLICATION));

        // 3. 좋아요 기록을 삭제합니다 (UserLikePosts).
        userLikePostsRepository.delete(userLikePosts);

        // 4. 게시물의 좋아요 수를 1 감소시킵니다 (PostCount).
        post.getPostCount().decreaseLikesCount();
    }

    // 조회수 + 1
    private void increaseViewCount(Post post) {
        PostCount postCount = post.getPostCount();
        postCount.increaseViewCount();
    }

    // 주간 top5 게시물 dto 반환
    @Override
    public PostTop5ResponseDto getWeeklyTop5Posts() {
        // 레디스에서 5개 값 가져옴.
        String weekly = getWeeklyRankingKey();
        List<Long> topPostsId = getTopPosts(weekly, 5);

        if (topPostsId.size() < 5) {
            // 인기글이 5개가 안되면 역대 인기 게시글 5개 반환
            Pageable pageable = PageRequest.of(0, 5);

            // 1. Fetch Join이 적용된 새로운 Repository 메서드를 호출
            Slice<Post> postSlice = postRepository.findSliceByOrderByViewCountDesc(pageable);

            // 2. 스트림 내에서 DTO를 변환하며 프로필 이미지 URL을 생성
            List<PostSummaryDto> posts = getPostSummaryDtos(postSlice.getContent());
            return new PostTop5ResponseDto(posts);
        }

        // --- N+1 문제 해결 부분 ---
        // 1. findAllById를 사용해 단 한 번의 쿼리로 모든 Post를 조회합니다.
        List<Post> topPosts = postRepository.findAllById(topPostsId);

        // 2. Redis의 랭킹 순서대로 조회된 Post 리스트를 재정렬합니다.
        // Map을 사용해 ID로 Post를 빠르게 찾을 수 있도록 변환 (O(N))
        Map<Long, Post> postMap = topPosts.stream()
                .collect(Collectors.toMap(Post::getId, post -> post));

        // Redis ID 순서에 맞춰 최종 리스트 생성
        List<Post> sortedTopPosts = topPostsId.stream()
                .map(postMap::get)
                .filter(Objects::nonNull) // 혹시 모를 null 값 방지
                .collect(Collectors.toList());


        // 2. 스트림 내에서 DTO를 변환하며 프로필 이미지 URL을 생성
        List<PostSummaryDto> posts = getPostSummaryDtos(sortedTopPosts);

        return new PostTop5ResponseDto(posts);
    }


    // 일일 top5 게시물 dto 반환
    @Override
    public PostTop5ResponseDto getDailyTop5Posts() {
        // 레디스에서 5개 값 가져옴.
        String dailyKey = getDailyRankingKey();
        List<Long> topPostsId = getTopPosts(dailyKey, 5);

        if (topPostsId.size() < 5) {
            // 인기글이 5개가 안되면 역대 인기 게시글 5개 반환
            Pageable pageable = PageRequest.of(0, 5);

            // 1. Fetch Join이 적용된 새로운 Repository 메서드를 호출
            Slice<Post> postSlice = postRepository.findSliceByOrderByViewCountDesc(pageable);

            // 2. 스트림 내에서 DTO를 변환하며 프로필 이미지 URL을 생성
            List<PostSummaryDto> posts = getPostSummaryDtos(postSlice.getContent());
            return new PostTop5ResponseDto(posts);
        }

        // --- N+1 문제 해결 부분 ---
        // 1. findAllById를 사용해 단 한 번의 쿼리로 모든 Post를 조회합니다.
        List<Post> topPosts = postRepository.findAllById(topPostsId);

        // 2. Redis의 랭킹 순서대로 조회된 Post 리스트를 재정렬합니다.
        // Map을 사용해 ID로 Post를 빠르게 찾을 수 있도록 변환 (O(N))
        Map<Long, Post> postMap = topPosts.stream()
                .collect(Collectors.toMap(Post::getId, post -> post));

        // Redis ID 순서에 맞춰 최종 리스트 생성
        List<Post> sortedTopPosts = topPostsId.stream()
                .map(postMap::get)
                .filter(Objects::nonNull) // 혹시 모를 null 값 방지
                .collect(Collectors.toList());

        // 2. 스트림 내에서 DTO를 변환하며 프로필 이미지 URL을 생성
        List<PostSummaryDto> posts = getPostSummaryDtos(sortedTopPosts);

        return new PostTop5ResponseDto(posts);
    }

    // top N 개의 게시물 id 조회
    private List<Long> getTopPosts(String key, long limit){
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        Set<Object> topPostIds = zSetOperations.reverseRange(key, 0, limit -1);

        if (topPostIds == null || topPostIds.isEmpty()) {
            return List.of();
        }

        return topPostIds.stream()
                .map(obj -> Long.parseLong(obj.toString())) // 👈 이 부분이 최종 수정안입니다.
                .collect(Collectors.toList());
    }

    // 일일 랭킹 키
    private String getDailyRankingKey() {
        return String.format("ranking:daily:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    // 주간 랭킹 키
    private String getWeeklyRankingKey() {
        LocalDate now = LocalDate.now();
        int weekOfYear = now.get(WeekFields.of(Locale.KOREA).weekOfWeekBasedYear());
        return "ranking:weekly:" + now.getYear() + "-W" + String.format("%02d", weekOfYear);
    }

    private List<PostSummaryDto> getPostSummaryDtos(List<Post> topPosts) {
        List<PostSummaryDto> posts = topPosts.stream()
                .map(post -> {
                    String profileImageUrl = null;
                    // Fetch Join으로 데이터를 모두 가져왔으므로 추가 쿼리가 발생하지 않습니다.
                    if (post.getUser() != null && post.getUser().getImage() != null) {
                        profileImageUrl = "https://" + cloudfrontDomain + "/" + post.getUser().getImage().getS3Key();
                    } else {
                        profileImageUrl = "https://" + cloudfrontDomain + "/" + defaultProfileImageKey;
                    }
                    return new PostSummaryDto(post, profileImageUrl);
                })
                .collect(Collectors.toList());
        return posts;
    }

}
