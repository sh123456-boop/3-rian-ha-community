package com.ktb.community.repository;

import com.ktb.community.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 댓글 작성시간 기준으로 정렬 후 반환
    //List<Comment> findAllByPostIdOrderByCreatedAtDesc(Long postId);

    // 최초 로딩을 위한 쿼리 (커서 X)
    // 게시물 ID로 최신 댓글부터 size만큼 조회
    //Slice<Comment> findByPostIdOrderByIdDesc(Long postId, Pageable pageable);

    // 다음 페이지 로딩을 위한 쿼리 (커서 O)
    // 특정 댓글 ID(커서)보다 작은 ID들을 최신순으로 size만큼 조회
    //Slice<Comment> findByPostIdAndIdLessThanOrderByIdDesc(Long postId, Long lastCommentId, Pageable pageable);

    // 👇 댓글 목록 조회를 위한 새로운 Fetch Join 메서드 1
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.user u " +
            "LEFT JOIN FETCH u.image " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.id DESC")
    Slice<Comment> findSliceByPostIdOrderByIdDesc(@Param("postId") Long postId, Pageable pageable);

    // 👇 댓글 목록 조회를 위한 새로운 Fetch Join 메서드 2
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.user u " +
            "LEFT JOIN FETCH u.image " +
            "WHERE c.post.id = :postId AND c.id < :lastCommentId " +
            "ORDER BY c.id DESC")
    Slice<Comment> findSliceByPostIdAndIdLessThanOrderByIdDesc(@Param("postId") Long postId, @Param("lastCommentId") Long lastCommentId, Pageable pageable);



}
