package com.ktb.community.dto.response;

import com.ktb.community.entity.Post;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
public class PostSummaryDto {

    private Long postId;
    private String title;
    private String authorNickname;
    private Instant createdAt;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private String authorProfileImageUrl;

    // Post 엔티티를 받아 DTO를 생성하는 생성자
    public PostSummaryDto(Post post, String authorProfileImageUrl) {
        this.postId = post.getId();
        this.title = post.getTitle();
        this.authorNickname = post.getUser().getNickname();
        this.createdAt = post.getCreatedAt();
        this.authorProfileImageUrl = authorProfileImageUrl;
        if (post.getPostCount() != null) {
            this.viewCount = post.getPostCount().getView_cnt();
            this.likeCount = post.getPostCount().getLikes_cnt();
            this.commentCount = post.getPostCount().getCmt_cnt();
        }
    }
}
