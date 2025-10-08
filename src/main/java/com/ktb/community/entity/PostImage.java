package com.ktb.community.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "post_images")
@IdClass(PostImageId.class) // ID 클래스 지정
public class PostImage {

    @Id // 복합 키 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Id // 복합 키 2
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(nullable = false)
    private int orders;
}
