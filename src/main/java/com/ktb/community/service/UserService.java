package com.ktb.community.service;

import com.ktb.community.dto.request.PasswordRequestDto;
import com.ktb.community.dto.response.LikedPostsResponseDto;
import com.ktb.community.dto.response.UserInfoResponseDto;

public interface UserService{

    // 회원 닉네임 수정
    void updateNickname(String nickname, Long userId);

    // 회원 닉네임 중복 검사
    boolean findNickname(String nickname);

    // 회원 비밀번호 수정
    void updatePassword(PasswordRequestDto dto, Long userId);

    // 회원 탈퇴
    void deleteUser(Long userId, String password);

    // 회원 프로필 이미지 설정
    void updateProfileImage(Long userId, String s3Key);

    // 프로필 이미지 삭제
    void deleteProfileImage(Long userId);

    // 사용자가 좋아요 누른 모든 게시물의 id목록을 최신순으로 반환
    LikedPostsResponseDto getLikedPosts(Long userId);

    // 사용자 정보 페이지
    UserInfoResponseDto getUserInfo(Long userId);


}
