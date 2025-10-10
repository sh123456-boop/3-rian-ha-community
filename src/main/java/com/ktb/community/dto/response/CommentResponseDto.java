package com.ktb.community.dto.response;

import com.ktb.community.entity.Comment;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CommentResponseDto {

    private Long id;
    private String contents;
    private String authorNickname;
    private Instant createdAt;

    // Comment 엔티티를 인자로 받는 생성자 (이 부분을 추가!)
    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.contents = comment.getContents();
        this.authorNickname = comment.getUser().getNickname();
        this.createdAt = comment.getCreatedAt();
    }
}
