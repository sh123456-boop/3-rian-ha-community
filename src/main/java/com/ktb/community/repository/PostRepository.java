package com.ktb.community.repository;

import com.ktb.community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {


    Slice<Post> findByOrderByIdDesc(Pageable pageable);

    // 다음 페이지 로딩: 특정 ID(커서)보다 작은 ID들을 내림차순으로 정렬하여 상위 N개 조회

    Slice<Post> findByIdLessThanOrderByIdDesc(Long lastPostId, Pageable pageable);


    // 👇 기존 메서드 대신 사용할 새로운 메서드 1
    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH u.image " +
            "ORDER BY p.id DESC")
    Slice<Post> findSliceByOrderByIdDesc(Pageable pageable);

    // 👇 기존 메서드 대신 사용할 새로운 메서드 2
    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH u.image " +
            "WHERE p.id < :lastPostId ORDER BY p.id DESC")
    Slice<Post> findSliceByIdLessThanOrderByIdDesc(@Param("lastPostId") Long lastPostId, Pageable pageable);

}
