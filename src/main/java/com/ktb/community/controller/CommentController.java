package com.ktb.community.controller;

import com.ktb.community.dto.request.CommentRequestDto;
import com.ktb.community.dto.response.CommentResponseDto;
import com.ktb.community.dto.response.CommentSliceResponseDto;
import com.ktb.community.service.CommentService;
import com.ktb.community.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Comment API", description = "댓글 도메인 API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @Operation(
            summary = "게시글 Read",
            description = "게시글의 ID를 파라미터로 보내면 해당하는 게시글 조회",
            parameters = {
                    @Parameter(
                            name = "id",
                            description = "조회할 게시글 ID",
                            required = true,
                            in = ParameterIn.PATH
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "실패"
                    )
            }
    )
    @PostMapping("/v1/posts/{postId}/comments")
    public ResponseEntity<Void> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        commentService.createComment(postId, userId, requestDto);

        // 반환은 post 상세 조회 페이지로
        return ResponseEntity.created(URI.create("/v1/posts/" + postId)).build();
    }

    // 댓글 조회(인피니티 스크롤)
    @GetMapping("/v1/posts/{postId}/comments")
    public ResponseEntity<CommentSliceResponseDto> getCommentsByCursor(
            @PathVariable Long postId,
            @RequestParam(required = false) Long lastCommentId) {

        CommentSliceResponseDto response = commentService.getCommentsByCursor(postId, lastCommentId);
        return ResponseEntity.ok(response);
    }

    // 댓글 수정
    @PatchMapping("/v1/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        commentService.updateComment(commentId, userId, requestDto);

        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        commentService.deleteComment(commentId, userId);

        return ResponseEntity.noContent().build();
    }

}
