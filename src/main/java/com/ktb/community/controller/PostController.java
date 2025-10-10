package com.ktb.community.controller;

import com.ktb.community.dto.request.PostCreateRequestDto;
import com.ktb.community.dto.response.PostResponseDto;
import com.ktb.community.dto.response.PostSliceResponseDto;
import com.ktb.community.service.CommentService;
import com.ktb.community.service.CustomUserDetails;
import com.ktb.community.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.file.AccessDeniedException;

@Tag(name = "Post API", description = "게시물 도메인 API")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    //게시글 작성
    @PostMapping("/v1/posts")
    public ResponseEntity<Void> createPost(@RequestBody @Valid PostCreateRequestDto dto,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        // spring security를 통해 현재 인증된 사용자의 정보를 가져옴
        Long userId = userDetails.getUserId();

        Long postId = postService.createPost(dto, userId);

        // 생성된 게시물의 uri를 location 헤더에 담아 201 created 응답을 보냄
        return ResponseEntity.created(URI.create("/v1/posts/" + postId)).build();
    }

    // 게시글 단건 조회
    @GetMapping("/v1/posts/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long postId) {
        PostResponseDto postResponseDto = postService.getPost(postId);

        return ResponseEntity.ok(postResponseDto);
    }

    // 게시글 전체 조회 (인피니티 스크롤)
    @GetMapping
    public ResponseEntity<PostSliceResponseDto> getPostSlice(
            @RequestParam(required = false) Long lastPostId) {

        PostSliceResponseDto response = postService.getPostSlice(lastPostId);
        return ResponseEntity.ok(response);
    }

    // 게시글삭제
    @DeleteMapping("/v1/posts/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails
                                           ) throws AccessDeniedException {
        // 현재 인증된 사용자의 ID를 가져옵니다.
        Long currentUserId = userDetails.getUserId();

        postService.deletePost(postId, currentUserId);

        // 성공적으로 삭제되었을 때 표준적인 응답은 204 No Content 입니다.
        return ResponseEntity.ok("게시글이 삭제되었습니다.");
    }

    // 게시글 수정
    @PutMapping("/v1/posts/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @RequestBody PostCreateRequestDto requestDto,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long currentUserId = userDetails.getUserId();
        postService.updatePost(postId, requestDto, currentUserId);

        return ResponseEntity.created(URI.create("/v1/posts/" + postId)).build();
    }

    // 게시글 좋아요 추가
    @PostMapping("/v1/posts/{id}/like")
    public ResponseEntity<Void> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 현재 로그인한 사용자의 ID를 가져옵니다.
        Long currentUserId = userDetails.getUserId();

        // 서비스 계층에 좋아요 추가 작업을 위임합니다.
        postService.likePost(postId, currentUserId);

        // 성공 시 200 OK 응답을 반환합니다.
        return ResponseEntity.ok().build();
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/v1/posts/{id}/like")
    public ResponseEntity<Void> unlikePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        postService.unlikePost(postId, currentUserId);

        // 성공 시 204 No Content 응답을 반환합니다.
        return ResponseEntity.noContent().build();
    }

}
