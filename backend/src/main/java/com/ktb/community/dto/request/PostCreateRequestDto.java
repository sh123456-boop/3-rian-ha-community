package com.ktb.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class PostCreateRequestDto {
    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력 항목입니다.")
    @Size(max = 10000, message = "내용은 10000자를 초과할 수 없습니다.") // 1000자 제한
    private String content;

    // 이미지 정보가 없으면 null일 수 있으므로 @NotNull은 제외
    @Size(max = 5, message = "이미지는 최대 5개까지 업로드할 수 있습니다.")
    private List<ImageInfo> images;

    // 내부 static 클래스로 이미지 정보를 받음
    public static class ImageInfo {
        @NotBlank(message = "S3 Key는 필수입니다.")
        private String s3_key;

        @NotNull(message = "이미지 순서는 필수입니다.")
        private int order;

        // Getter
        public String getS3_key() {
            return s3_key;
        }

        public int getOrder() {
            return order;
        }
    }
}
