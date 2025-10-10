package com.ktb.community.controller;

import com.ktb.community.dto.request.PreSignedUrlRequestDto;
import com.ktb.community.dto.response.PreSignedUrlResponseDto;
import com.ktb.community.service.CustomUserDetails;
import com.ktb.community.service.S3Service;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "S3 API", description = "s3 도메인 API")
@RestController
@RequiredArgsConstructor
public class S3urlController {

    private final S3Service s3Service;

    @PostMapping("/v1/posts/presignedUrl")
    public ResponseEntity<PreSignedUrlResponseDto> getUrl(
            @RequestBody @Valid PreSignedUrlRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        // spring security를 통해 현재 인증된 사용자의 정보를 가져옴
        Long userId = userDetails.getUserId();

        PreSignedUrlResponseDto urlResponseDto = s3Service.getPostPresignedPutUrl(userId, dto.getFileName());
        return ResponseEntity.ok(urlResponseDto);
    }

    @PostMapping("/v1/profiles/presignedUrl")
    public ResponseEntity<PreSignedUrlResponseDto> getProfileUrl (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PreSignedUrlRequestDto requestDto
    ){
        Long userId = userDetails.getUserId();
        PreSignedUrlResponseDto urlResponseDto = s3Service.getProfileImagePresignedUrl(userId, requestDto.getFileName());
        return ResponseEntity.ok(urlResponseDto);

    }

}
