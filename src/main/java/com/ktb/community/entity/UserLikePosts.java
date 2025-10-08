package com.ktb.community.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "user_like_posts")
@IdClass(UserLikePostsId.class) // ID 클래스 지정
@EntityListeners(AuditingEntityListener.class) // CreatedDate 활성화
public class UserLikePosts {

    @Id // 복합 키 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id // 복합 키 2
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @CreatedDate
    @Column(nullable = false)
    private Instant liked_at;
}
