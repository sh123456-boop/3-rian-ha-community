package com.ktb.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreSignedUrlResponseDto {
    String s3_key;
    String preSignedUrl;
}
