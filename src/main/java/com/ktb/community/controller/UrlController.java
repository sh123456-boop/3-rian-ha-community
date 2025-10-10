package com.ktb.community.controller;

import com.ktb.community.dto.request.PreSignedUrlRequestDto;
import com.ktb.community.dto.response.PreSignedUrlResponseDto;
import com.ktb.community.service.CustomUserDetails;
import com.ktb.community.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final S3Service s3Service;

    @GetMapping("/v1/presignedUrl")
    public ResponseEntity<PreSignedUrlResponseDto> getUrl(
            @RequestBody @Valid PreSignedUrlRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        // spring security를 통해 현재 인증된 사용자의 정보를 가져옴
        Long userId = userDetails.getUserId();

        PreSignedUrlResponseDto urlResponseDto = s3Service.getPresignedPutUrl(userId, dto.getFileName());
        return ResponseEntity.ok(urlResponseDto);
    }
}
