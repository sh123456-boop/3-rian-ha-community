package com.ktb.community.service;

import com.ktb.community.dto.request.PasswordRequestDto;
import com.ktb.community.dto.response.LikedPostsResponseDto;
import com.ktb.community.entity.Image;
import com.ktb.community.entity.User;
import com.ktb.community.entity.UserLikePosts;
import com.ktb.community.repository.UserLikePostsRepository;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final S3Service s3Service;
    private final UserLikePostsRepository userLikePostsRepository; // 리포지토리 주입


    // 회원 닉네임 수정
    public void updateNickname(String nickname, Long userId) {
        // 1. 닉네임 중복 확인
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 닉네임 업데이트
        user.updateNickname(nickname);
    }

    // 회원 비밀번호 수정
    public void updatePassword(PasswordRequestDto dto, Long userId) {

        // 1. 새 비밀번호와 확인용 비밀번호가 일치하는지 확인
        if (!dto.getPassword().equals(dto.getRePassword())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 2. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 새 비밀번호를 암호화하여 업데이트
        user.updatePassword(bCryptPasswordEncoder.encode(dto.getPassword()));
    }


    // 회원 탈퇴
    public void deleteUser(Long userId, String password) {
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 비밀번호 확인
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 사용자 삭제
        userRepository.delete(user);
    }

    // 회원 프로필 이미지 설정
    /**
     * 사용자의 프로필 이미지를 업데이트합니다.
     * 기존 이미지가 있으면 S3와 DB에서 삭제하고, 새로운 이미지로 교체합니다.
     * @param userId 현재 사용자 ID
     * @param s3Key S3에 업로드된 새로운 이미지의 Key
     */
    @Transactional
    public void updateProfileImage(Long userId, String s3Key) {
        // 1. 사용자 정보를 조회합니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 기존 프로필 이미지가 있는지 확인합니다.
        Image oldImage = user.getImage();
        if (oldImage != null) {
            // 3. 있었다면 기존 이미지를 S3 버킷에서 삭제합니다.
            s3Service.deleteFile(oldImage.getS3_key());
        }

        // 4. 새로운 Image 엔티티를 생성하고 사용자 정보(user)를 넣어줍니다.
        Image newProfileImage = new Image(s3Key, user);

        // 5. 사용자의 프로필 이미지를 새로운 이미지로 교체
        // 이 한 줄로 인해 orphanRemoval(기존 이미지 삭제)과 cascade(새 이미지 저장)가 모두 동작
        user.updateProfileImage(newProfileImage);
    }

    // 프로필 이미지 삭제
    public void deleteProfileImage(Long userId) {
        // 1. 사용자 정보를 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 사용자의 현재 프로필 이미지를 확인
        Image profileImage = user.getImage();

        // 3. 프로필 이미지가 존재할 경우에만 삭제 로직을 실행
        if (profileImage != null) {
            // 3-1. S3 버킷에서 실제 이미지 파일을 삭제
            s3Service.deleteFile(profileImage.getS3_key());

            // 3-2. User 엔티티와 Image 엔티티의 연관관계를 끊음
            user.updateProfileImage(null);
        }
    }

    /**
     * 사용자가 좋아요를 누른 모든 게시물의 ID 목록을 최신순으로 반환합니다.
     * @param userId 현재 사용자 ID
     * @return 게시물 ID의 List<Long>
     */
    @Transactional(readOnly = true) // 단순 조회이므로 readOnly = true 옵션으로 성능 최적화
    public LikedPostsResponseDto getLikedPosts(Long userId) {
        // 1. 사용자 정보를 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 해당 사용자의 '좋아요' 기록을 liked_at 최신순으로 조회
        List<UserLikePosts> likedPosts = userLikePostsRepository.findByUserOrderByLikedAtDesc(user);

        // 3. 조회된 '좋아요' 기록 리스트에서 Post ID만 추출하여 새로운 리스트로 만듭니다.
        List<Long> postIds = likedPosts.stream()
                .map(userLikePosts -> userLikePosts.getPost().getId())
                .collect(Collectors.toList());

        return new LikedPostsResponseDto(postIds);
    }




}
