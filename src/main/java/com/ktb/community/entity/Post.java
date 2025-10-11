package com.ktb.community.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class Post extends Timestamped{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @NotBlank // null, "", " " 모두 허용하지 않음
    @Size(max = 100) // 제목의 최대 길이를 100자로 제한
    @Column(nullable = false, length = 100) // DB 레벨에서 NOT NULL, 길이 100 제약조건
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contents;

    // Post(Many) to User(One) 관계
    // 게시글은 반드시 유저를 가져야 하므로 nullable = false
    @ManyToOne(fetch = FetchType.LAZY) // 성능 최적화를 위해 LAZY 로딩 사용
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> commentList = new ArrayList<>();

    // 게시글에 포함된 이미지 목록
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orders ASC") // 이미지 순서(orders)에 따라 오름차순으로 정렬
    @Builder.Default
    private List<PostImage> postImageList = new ArrayList<>();

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private PostCount postCount = new PostCount();

    // Post 생성자에 PostCount 초기화 로직 추가
    @PrePersist
    public void prePersist() {
        if (this.postCount != null) {
            this.postCount.setPost(this); // 👈 가장 중요한 부분!
        }
    }

    // update 메서드
    public void update(String title, String content) {
        this.title = title;
        this.contents = content;
    }

    // +++++++++++++++++++++ 연관관계 메서드 +++++++++++++++++++++ //

    // post <-> user
    public void setUser(User user) {
        this.user = user;
        user.getPostList().add(this);
    }

    // post <-> postImage
    public void setPostImageList(PostImage postImage) {
        this.postImageList.add(postImage);
    }

    // post <-> comment
    public void setCommentList(Comment comment) {
        this.commentList.add(comment);
    }

    public void addPostImage(PostImage postImage) {
        this.postImageList.add(postImage);
    }

}
