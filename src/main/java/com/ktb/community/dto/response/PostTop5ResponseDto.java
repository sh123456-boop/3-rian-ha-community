package com.ktb.community.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class PostTop5ResponseDto {
    List<PostSummaryDto> posts;

    public PostTop5ResponseDto(List<PostSummaryDto> posts) {
        this.posts = posts;
    }
}
