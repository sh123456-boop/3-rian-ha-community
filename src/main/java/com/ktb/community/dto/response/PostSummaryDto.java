package com.ktb.community.dto.response;

import com.ktb.community.entity.Post;

import java.time.Instant;

public class PostSummaryDto {

    private Long postId;
    private String title;
    private String authorNickname;
    private Instant createdAt;
    private int viewCount;
    private int likeCount;
    private int commentCount;

    // Post 엔티티를 받아 DTO를 생성하는 생성자
    public PostSummaryDto(Post post) {
        this.postId = post.getId();
        this.title = post.getTitle();
        this.authorNickname = post.getUser().getNickname();
        this.createdAt = post.getCreatedAt();

        if (post.getPostCount() != null) {
            this.viewCount = post.getPostCount().getView_cnt();
            this.likeCount = post.getPostCount().getLikes_cnt();
            this.commentCount = post.getPostCount().getCmt_cnt();
        }
    }
}
