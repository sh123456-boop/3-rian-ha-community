package com.ktb.community.service;

import com.ktb.community.dto.response.PreSignedUrlResponseDto;

public interface S3Service {

    // 파일 업로드용 Presigned URL 생성
    PreSignedUrlResponseDto getPostPresignedPutUrl(Long userId, String fileName);

    // 유저 프로필용 Presigned Url 생성
    PreSignedUrlResponseDto getProfileImagePresignedUrl(Long userId, String fileName);

    // s3 파일 삭제
    void deleteFile(String s3Key);


}
