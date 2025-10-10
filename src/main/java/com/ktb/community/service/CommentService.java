package com.ktb.community.service;

import com.ktb.community.dto.request.CommentRequestDto;
import com.ktb.community.dto.response.CommentResponseDto;
import com.ktb.community.dto.response.CommentSliceResponseDto;
import com.ktb.community.entity.Comment;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.repository.CommentRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private static final int PAGE_SIZE = 20; // 한 번에 불러올 댓글 수


    // 댓글 작성
    public void createComment(Long postId, Long userId, CommentRequestDto dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Comment comment = new Comment(dto.getContents(), post, user);
        post.setCommentList(comment); // 연관관계 메서드
        commentRepository.save(comment);

        // 게시물의 댓글 수 1 증가
        post.getPostCount().increaseCmtCount();
    }

    // 커서 기반 댓글 조회
    @Transactional(readOnly = true)
    public CommentSliceResponseDto getCommentsByCursor(Long postId, Long lastCommentId) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        Slice<Comment> commentSlice = (lastCommentId == null)
                ? commentRepository.findByPostIdOrderByIdDesc(postId, pageable)
                : commentRepository.findByPostIdAndIdLessThanOrderByIdDesc(postId, lastCommentId, pageable);

        List<CommentResponseDto> comments = commentSlice.getContent().stream()
                .map(CommentResponseDto::new)
                .collect(Collectors.toList());

        return new CommentSliceResponseDto(comments, commentSlice.hasNext());
    }


    // 댓글 수정
    public void updateComment(Long commentId, Long userId, CommentRequestDto dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("댓글을 수정할 권한이 없습니다.");
        }

        comment.update(dto.getContents());
    }

    // 댓글 삭제
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("댓글을 삭제할 권한이 없습니다.");
        }

        // 게시물의 댓글 수 1 감소
        comment.getPost().getPostCount().decreaseCmtCount();

        // 2. Post의 commentList에서도 Comment 제거 (객체 상태 일관성)
        comment.getPost().getCommentList().remove(comment);

        commentRepository.delete(comment);
    }
}
