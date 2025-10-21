package com.ktb.community.service;

import com.ktb.community.dto.request.CommentRequestDto;
import com.ktb.community.dto.response.CommentResponseDto;
import com.ktb.community.dto.response.CommentSliceResponseDto;

public interface CommentService {

    // 댓글 작성
    CommentResponseDto createComment(Long postId, Long userId, CommentRequestDto dto);

    // 커서 기반 댓글dto 리턴
    CommentSliceResponseDto getCommentsByCursor(Long postId, Long lastCommentId);

    // 댓글 수정
    void updateComment(Long commentId, Long userId, CommentRequestDto dto);

    // 댓글 삭제
    void deleteComment(Long commentId, Long userId);
}
