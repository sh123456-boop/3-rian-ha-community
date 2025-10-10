package com.ktb.community.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class LikedPostsResponseDto {
    private List<Long> postIds;
    private int totalCount;

    public LikedPostsResponseDto(List<Long> postIds) {
        this.postIds = postIds;
        this.totalCount = postIds.size();
    }
}
