package com.ktb.community.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class PostSliceResponseDto {

    private List<PostSummaryDto> posts;
    private boolean hasNext;

    public PostSliceResponseDto(List<PostSummaryDto> posts, boolean hasNext) {
        this.posts = posts;
        this.hasNext = hasNext;
    }

}
