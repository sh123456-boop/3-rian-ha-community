package com.ktb.community.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class PostImageId implements Serializable {

    private Long post; // Post 엔티티의 id 필드명과 일치
    private Long image; // Image 엔티티의 id 필드명과 일치
}
