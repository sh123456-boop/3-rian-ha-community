package com.ktb.community.controller;

import com.ktb.community.dto.ApiResponseDto;
import com.ktb.community.dto.request.CommentRequestDto;
import com.ktb.community.dto.response.CommentResponseDto;
import com.ktb.community.dto.response.CommentSliceResponseDto;
import com.ktb.community.service.CommentService;
import com.ktb.community.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment API", description = "댓글 도메인 API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @Operation(
            summary = "댓글 작성",
            description = "특정 게시글에 현재 로그인된 사용자의 댓글을 작성합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "postId",
                            in = ParameterIn.PATH,
                            description = "댓글을 작성할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "댓글 내용을 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "댓글 작성 성공",
                            headers = {
                                    @Header(
                                            name = "Location",
                                            description = "댓글이 달린 게시글의 URI (예: /v1/posts/42)",
                                            schema = @Schema(type = "string")
                                    )
                            },
                            content = @Content // 성공 시 응답 본문은 없음
                    )
            }
    )
    @PostMapping("/v1/posts/{postId}/comments")
    public ApiResponseDto<Object> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        CommentResponseDto comment = commentService.createComment(postId, userId, requestDto);
        return ApiResponseDto.success("댓글이 저장되었습니다.");


    }

    // 댓글 조회(인피니티 스크롤)
    @Operation(
            summary = "댓글 목록 조회 (무한 스크롤)",
            description = "특정 게시글의 댓글 목록을 무한 스크롤(Cursor-based pagination) 방식으로 조회합니다. 최초 조회 시에는 lastCommentId를 보내지 않고, 이후 요청부터는 이전에 받은 마지막 댓글의 ID를 lastCommentId로 보내주세요.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "postId",
                            in = ParameterIn.PATH,
                            description = "댓글 목록을 조회할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    ),
                    @Parameter(
                            name = "lastCommentId",
                            in = ParameterIn.QUERY,
                            description = "조회의 시작점이 될 마지막 댓글의 ID. 최초 조회 시에는 생략합니다.",
                            required = false,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommentSliceResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping("/v1/posts/{postId}/comments")
    public ApiResponseDto<Object> getCommentsByCursor(
            @PathVariable Long postId,
            @RequestParam(required = false) Long lastCommentId) {

        CommentSliceResponseDto response = commentService.getCommentsByCursor(postId, lastCommentId);
        return ApiResponseDto.success(response);
    }

    // 댓글 수정
    @Operation(
            summary = "댓글 수정",
            description = "댓글 ID를 사용하여 현재 로그인된 사용자의 댓글 내용을 수정합니다. 자신의 댓글만 수정할 수 있으며, 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "commentId",
                            in = ParameterIn.PATH,
                            description = "수정할 댓글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 댓글 내용을 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 수정 성공",
                            content = @Content // 성공 시 응답 본문은 없음
                    )
            }
    )
    @PutMapping("/v1/comments/{commentId}")
    public ApiResponseDto<Object> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        commentService.updateComment(commentId, userId, requestDto);

        return ApiResponseDto.success("댓글이 수정되었습니다.");
    }


    // 댓글 삭제
    @Operation(
            summary = "댓글 삭제",
            description = "댓글 ID를 사용하여 현재 로그인된 사용자의 댓글을 삭제합니다. 자신의 댓글만 삭제할 수 있으며, 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "commentId",
                            in = ParameterIn.PATH,
                            description = "삭제할 댓글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "댓글 삭제 성공. 응답 본문은 없습니다.",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/v1/comments/{commentId}")
    public ApiResponseDto<Object> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        commentService.deleteComment(commentId, userId);

        return ApiResponseDto.success("댓글이 삭제되었습니다.");
    }

}
