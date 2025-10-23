package com.ktb.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentRequestDto {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 1000, message = "내용은 1000자를 초과할 수 없습니다.")
    private String contents;

}
