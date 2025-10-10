package com.ktb.community.repository;

import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.entity.UserLikePosts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLikePostsRepository extends JpaRepository<UserLikePosts, Long> {
    boolean existsByUserAndPost(User user, Post post);
    Optional<UserLikePosts> findByUserAndPost(User user, Post post);

    // 사용자가 좋아요 누른 게시물 목록을 liked_at 기준으로 정렬하여 찾는 메서드
    List<UserLikePosts> findByUserOrderByLikedAtDesc(User user);
}
