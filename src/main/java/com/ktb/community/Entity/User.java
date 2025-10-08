package com.ktb.community.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Table(name = "users")
public class User extends Timestamped{

    @Id
    @GeneratedValue
    @Column(name = "user_id")
    private Long id;

    @NotBlank // null, 빈문자열, 공백 문자열 모두 허용하지 않음
    @Email // 이메일 형식 검증
    @Column(nullable = false, unique = true) // DB 레벨에서 NOT NULL, UNIQUE 제약조건 추가
    private String email;

    @NotBlank
    @Size(min = 8, max = 20) // 비밀번호 길이 제한
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Size(min = 2, max = 10)
    @Column(nullable = false, unique = true)
    private String nickname;

    // 유저를 삭제하면 관련 image 삭제, 이미지를 바꾸면 기존 이미지 삭제
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Image image;

    // Post와의 일대다 관계 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> postList = new ArrayList<>();

    // 한 명의 유저는 여러 '좋아요'를 누를 수 있음 (OneToMany)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLikePosts> likesList = new ArrayList<>();
}