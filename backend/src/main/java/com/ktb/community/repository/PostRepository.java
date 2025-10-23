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


    // 👇 기존 메서드 대신 사용할 새로운 메서드 1
    @Query("SELECT DISTINCT p FROM Post p " +
                  "LEFT JOIN FETCH p.user u " +
                  "LEFT JOIN FETCH u.image " +
                  "LEFT JOIN FETCH p.postCount c " + // 👈 postcount -> postCount (필드명 수정)
                  "ORDER BY c.view_cnt DESC, p.id DESC") // 👈 2차 정렬 기준 추가
    Slice<Post> findSliceByOrderByViewCountDesc(Pageable pageable);

    // 👇 기존 메서드 대신 사용할 새로운 메서드 2
    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH u.image " +
            "LEFT JOIN FETCH p.postCount c " + // 👈 postcount -> postCount (필드명 수정)
            "WHERE (c.view_cnt < :lastViewCount) OR (c.view_cnt = :lastViewCount AND p.id < :lastPostId) " + // 👈 페이지네이션 조건 수정
            "ORDER BY c.view_cnt DESC, p.id DESC") // 👈 postcount -> view_cnt, 2차 정렬 기준 추가
    Slice<Post> findSliceByOrderByViewCountDesc(@Param("lastViewCount") Long lastViewCount, @Param("lastPostId") Long lastPostId, Pageable pageable);


    // ✅ N+1 테스트를 위한 새로운 메서드 (Fetch Join 없음)
    @Query("SELECT p FROM Post p ORDER BY p.id DESC")
    Slice<Post> findSliceWithoutFetchJoinOrderByIdDesc(Pageable pageable);

    // @Query("SELECT p FROM Post p ORDER BY p.id DESC")


}
