package com.ktb.community.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode // equals와 hashCode를 자동으로 생성해 줌
public class UserLikePostsId implements Serializable {
    private Long user;
    private Long post;
}
