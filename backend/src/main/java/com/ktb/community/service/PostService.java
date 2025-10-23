package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequestDto;
import com.ktb.community.dto.response.PostTop5ResponseDto;
import com.ktb.community.dto.response.PostResponseDto;
import com.ktb.community.dto.response.PostSliceResponseDto;

import java.nio.file.AccessDeniedException;

public interface PostService {
    // 게시글 작성
    Long createPost(PostCreateRequestDto dto, Long userId);

    // 게시글 단건 조회(상세 페이지)
    PostResponseDto getPost(Long postId);

    // 게시글 전체 조회(인피니티 스크롤)
    PostSliceResponseDto getPostSlice(Long lastPostId);

    // 게시글 인기순 전체 조회(인피니티 스크롤)
    PostSliceResponseDto getPopularPostSlice(Long lastViewCount, Long lastPostId);

    // 게시글 수정
    void updatePost(Long postId, PostCreateRequestDto requestDto, Long userId);

    // 게시글 삭제
    void deletePost(Long postId, Long userId) throws AccessDeniedException;

    // 게시글 좋아요 추가 메서드
    void likePost(Long postId, Long userId);

    // 게시글 좋아요 취소 메서드
    void unlikePost(Long postId, Long userId);

    // 일일 인기 게시글 top5 반환
    PostTop5ResponseDto getDailyTop5Posts();

    // 주간 인기 게시글 top5 반환
    PostTop5ResponseDto getWeeklyTop5Posts();
}