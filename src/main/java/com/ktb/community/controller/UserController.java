package com.ktb.community.controller;

import com.ktb.community.dto.request.*;
import com.ktb.community.dto.response.LikedPostsResponseDto;
import com.ktb.community.dto.response.PreSignedUrlResponseDto;
import com.ktb.community.dto.response.UserInfoResponseDto;
import com.ktb.community.service.CustomUserDetails;
import com.ktb.community.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    @Operation(
            summary = "닉네임 변경",
            description = "현재 로그인된 사용자의 닉네임을 변경하는 로직. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 닉네임을 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NicknameRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "닉네임 변경 성공"
                    )
            }
    )
    @PutMapping("/v1/users/me/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NicknameRequestDto dto
    ) {
        Long userId = userDetails.getUserId();
        userService.updateNickname(dto.getNickname(), userId);

        return ResponseEntity.ok().build();
    }

    // 유저 닉네임이 중복되었나 확인하는 요청
    @GetMapping("/v1/users/me/nickname")
    public ResponseEntity<Boolean> findNickname(@Valid @RequestParam String nickname) {
        boolean result = userService.findNickname(nickname);
        // 닉네임 변경 가능
        if (result) return ResponseEntity.ok(true);
        // 닉네임 변경 불가능
        else return ResponseEntity.ok(false);
    }

    // 비밀번호 수정
    @Operation(
            summary = "비밀번호 변경",
            description = "현재 로그인된 사용자의 비밀번호를 변경하는 로직. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 비밀번호를 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "비밀번호 변경 성공"
                    )
            }
    )
    @PutMapping("/v1/users/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordRequestDto requestDto) {

        Long currentUserId = userDetails.getUserId();
        userService.updatePassword(requestDto, currentUserId);

        return ResponseEntity.ok().build();
    }

    // 회원 탈퇴
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인된 사용자의 계정을 탈퇴 처리합니다. 계정 소유자 확인을 위해 반드시 현재 비밀번호를 요청 본문에 포함해야 합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "회원 탈퇴 성공",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "비밀번호 불일치: 요청 본문에 전달된 비밀번호가 실제 사용자의 비밀번호와 다릅니다.",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/v1/users/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserDeleteRequestDto requestDto) {

        Long currentUserId = userDetails.getUserId();
        userService.deleteUser(currentUserId, requestDto.getPassword());

        return ResponseEntity.noContent().build();
    }

    // 유저 프로필 이미지 추가
    @Operation(
            summary = "유저 프로필 이미지 생성",
            description = "s3에 이미지를 올릴 수 있습니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "s3_key를 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileImageRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "유저 프로필 이미지 생성 성공"
                    )
            }
    )
    @PostMapping("/v1/users/me/image")
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ProfileImageRequestDto requestDto) {

        Long userId = userDetails.getUserId();
        userService.updateProfileImage(userId, requestDto.getS3_key());

        return ResponseEntity.ok().build();
    }

    // 유저 프로필 이미지 삭제
    @Operation(
            summary = "유저 프로필 이미지 삭제",
            description = "s3에서 이미지를 삭제합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "유저 프로필 이미지 삭제 성공"
                    )
            }
    )
    @DeleteMapping("/v1/users/me/image")
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        userService.deleteProfileImage(userId);

        return ResponseEntity.noContent().build();
    }

    // 좋아요 게시물 id 반환
    @Operation(
            summary = "내가 '좋아요' 누른 게시글 목록 조회",
            description = "현재 로그인된 사용자가 '좋아요'를 누른 모든 게시글의 ID 목록을 조회합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "좋아요 누른 게시글 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LikedPostsResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패: 유효하지 않은 Access Token",
                            content = @Content
                    )
            }
    )
    @GetMapping("/v1/users/me/liked-posts")
    public ResponseEntity<LikedPostsResponseDto> getMyLikedPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        LikedPostsResponseDto responseDto = userService.getLikedPosts(currentUserId);

        return ResponseEntity.ok(responseDto);
    }

    // 사용자 닉네임, 프로필 이미지 수정 페이지
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인된 사용자의 정보(닉네임, 프로필 이미지 URL)를 조회합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "사용자 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserInfoResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping("/v1/users/me")
    public ResponseEntity<UserInfoResponseDto> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        UserInfoResponseDto userInfo = userService.getUserInfo(userId);
        return ResponseEntity.ok(userInfo);
    }

}
