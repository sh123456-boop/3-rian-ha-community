package com.ktb.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class PostResponseDto {
    private Long postId;
    private String title;
    private String content;
    private String nickname;
    private Instant updatedAt;
    private List<ImageInfo> images; // 이미지 정보 리스트를 내부에 포함


    // 재사용을 위한 내부 DTO (또는 별도 파일로 분리 가능)
    public static class ImageInfo {
        private String imageUrl; // 최종 CloudFront URL
        private int order;

        public ImageInfo(String imageUrl, int order) {
            this.imageUrl = imageUrl;
            this.order = order;
        }
    }
}
