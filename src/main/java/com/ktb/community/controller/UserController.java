package com.ktb.community.controller;

import com.ktb.community.dto.request.NicknameRequestDto;
import com.ktb.community.dto.request.PasswordRequestDto;
import com.ktb.community.dto.request.ProfileImageRequestDto;
import com.ktb.community.dto.request.UserDeleteRequestDto;
import com.ktb.community.dto.response.LikedPostsResponseDto;
import com.ktb.community.service.CustomUserDetails;
import com.ktb.community.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@Tag(name = "User API", description = "사용자 도메인 API")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 닉네임 수정
    @PutMapping("/v1/users/me/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NicknameRequestDto dto
    ) {
        Long userId = userDetails.getUserId();
        userService.updateNickname(dto.getNickname(), userId);

        return ResponseEntity.ok().build();
    }

    // 비밀번호 수정
    @PutMapping("/v1/users/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordRequestDto requestDto) {

        Long currentUserId = userDetails.getUserId();
        userService.updatePassword(requestDto, currentUserId);

        return ResponseEntity.ok().build();
    }

    // 회원 탈퇴
    @DeleteMapping("/v1/users/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserDeleteRequestDto requestDto) {

        Long currentUserId = userDetails.getUserId();
        userService.deleteUser(currentUserId, requestDto.getPassword());

        return ResponseEntity.noContent().build();
    }

    // 유저 프로필 이미지 추가
    @PostMapping("/v1/users/me/image")
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ProfileImageRequestDto requestDto) {

        Long userId = userDetails.getUserId();
        userService.updateProfileImage(userId, requestDto.getS3_key());

        return ResponseEntity.ok().build();
    }

    // 유저 프로필 이미지 삭제
    @DeleteMapping("/v1/users/me/image")
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        userService.deleteProfileImage(userId);

        return ResponseEntity.noContent().build();
    }

    // 좋아요 게시물 id 반환
    @GetMapping("/v1/users/me/liked-posts")
    public ResponseEntity<LikedPostsResponseDto> getMyLikedPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        LikedPostsResponseDto responseDto = userService.getLikedPosts(currentUserId);

        return ResponseEntity.ok(responseDto);
    }
}
