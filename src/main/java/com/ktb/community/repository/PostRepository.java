package com.ktb.community.repository;

import com.ktb.community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 최초 로딩: ID 내림차순으로 정렬하여 상위 N개 조회
    Slice<Post> findByOrderByIdDesc(Pageable pageable);

    // 다음 페이지 로딩: 특정 ID(커서)보다 작은 ID들을 내림차순으로 정렬하여 상위 N개 조회
    Slice<Post> findByIdLessThanOrderByIdDesc(Long lastPostId, Pageable pageable);
}
