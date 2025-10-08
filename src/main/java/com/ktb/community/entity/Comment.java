package com.ktb.community.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment extends Timestamped{

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String contents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
