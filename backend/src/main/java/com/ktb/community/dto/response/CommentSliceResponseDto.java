package com.ktb.community.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class CommentSliceResponseDto {

    private List<CommentResponseDto> comments;
    private boolean hasNext;

    public CommentSliceResponseDto(List<CommentResponseDto> comments, boolean hasNext) {
        this.comments = comments;
        this.hasNext = hasNext;
    }
}
