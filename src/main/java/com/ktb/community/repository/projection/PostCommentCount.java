package com.ktb.community.repository.projection;

public interface PostCommentCount {
    Long getPostId();
    int getCommentCount();
}
