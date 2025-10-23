package com.ktb.community.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "counts")
@Getter
@NoArgsConstructor
public class PostCount {

    @Id // 기본 키로 지정
    private Long id;

    @Setter
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // id 필드를 PostCount의 PK이자 Post의 FK로 매핑
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    @ColumnDefault("0") // DB 레벨에서 기본값을 0으로 설정
    private int likes_cnt;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int cmt_cnt;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int view_cnt;


    //== 비즈니스 로직 ==//
    // 조회수 +1
    public void increaseViewCount() {
        this.view_cnt += 1;
    }

    // 좋아요 + 1
    public void increaseLikesCount() {
        this.likes_cnt += 1;
    }

    // 좋아요 -1
    public void decreaseLikesCount() {
        this.likes_cnt = Math.max(0, this.likes_cnt - 1); // 음수가 되지 않도록 방지
    }

    public void increaseCmtCount() {
        this.cmt_cnt += 1;
    }
    public void decreaseCmtCount() {
        this.cmt_cnt = Math.max(0, this.cmt_cnt - 1);
    }

}
