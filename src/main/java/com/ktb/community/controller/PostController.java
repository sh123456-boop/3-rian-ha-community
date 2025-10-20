package com.ktb.community.controller;

import com.ktb.community.dto.ApiResponseDto;
import com.ktb.community.dto.request.PostCreateRequestDto;
import com.ktb.community.dto.response.PostResponseDto;
import com.ktb.community.dto.response.PostSliceResponseDto;
import com.ktb.community.service.CommentService;
import com.ktb.community.service.CustomUserDetails;
import com.ktb.community.service.PostService;
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
    @Operation(
            summary = "게시글 작성",
            description = "제목, 내용 및 이미지 정보(S3 Key, 순서)를 받아 새로운 게시글을 작성합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "게시글 생성을 위한 제목, 내용, 이미지 정보를 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostCreateRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "게시글 작성 성공",
                            headers = {
                                    @Header(
                                            name = "Location",
                                            description = "생성된 게시글의 URI (예: /v1/posts/42)",
                                            schema = @Schema(type = "string")
                                    )
                            },
                            content = @Content // 성공 시 응답 본문은 없음
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검사 실패 (제목/내용 누락 등)",
                            content = @Content
                    )
            }
    )
    @PostMapping("/v1/posts")
    public ApiResponseDto<Object> createPost(@RequestBody @Valid PostCreateRequestDto dto,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        // spring security를 통해 현재 인증된 사용자의 정보를 가져옴
        Long userId = userDetails.getUserId();

        Long postId = postService.createPost(dto, userId);

        // 생성된 게시물의 uri를 location 헤더에 담아 201 created 응답을 보냄
        return ApiResponseDto.success("게시글이 저장되었습니다.");
    }

    // 게시글 단건 조회
    @Operation(
            summary = "게시글 단건 조회",
            description = "게시글 ID를 사용하여 특정 게시글의 상세 정보를 조회합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            description = "조회할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "게시글 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PostResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 게시글입니다.",
                            content = @Content
                    )
            }
    )
    @GetMapping("/v1/posts/{id}")
    public ApiResponseDto<PostResponseDto> getPost(@PathVariable("id") Long postId) {
        PostResponseDto postResponseDto = postService.getPost(postId);

        return ApiResponseDto.success(postResponseDto);
    }


    // 게시글 전체 조회 (인피니티 스크롤)
    @Operation(
            summary = "게시글 목록 조회 (무한 스크롤)",
            description = "무한 스크롤(Cursor-based pagination) 방식으로 게시글 목록을 조회합니다. 최초 조회 시에는 lastPostId를 보내지 않고, 이후 요청부터는 이전에 받은 마지막 게시글의 ID를 lastPostId로 보내주세요.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "lastPostId",
                            in = ParameterIn.QUERY,
                            description = "조회의 시작점이 될 마지막 게시글의 ID. 최초 조회 시에는 생략합니다.",
                            required = false,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "게시글 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PostSliceResponseDto.class)
                            )
                    )
            }
    )
    @GetMapping("v1/posts")
    public ApiResponseDto<PostSliceResponseDto> getPostSlice(
            @RequestParam(required = false) Long lastPostId) {

        PostSliceResponseDto response = postService.getPostSlice(lastPostId);
        return ApiResponseDto.success(response);
    }

    @GetMapping("v1/posts/popular")
    public ApiResponseDto<PostSliceResponseDto> getPopularPostSlice(
            @RequestParam(required = false) Long lastViewCount,
            @RequestParam(required = false) Long lastPostId) {

        PostSliceResponseDto response = postService.getPopularPostSlice(lastViewCount, lastPostId);
        return ApiResponseDto.success(response);
    }

    // 게시글삭제
    @Operation(
            summary = "게시글 삭제",
            description = "게시글 ID를 사용하여 현재 로그인된 사용자의 게시글을 삭제합니다. 자신의 게시글만 삭제할 수 있으며, 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            description = "삭제할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "게시글 삭제 성공",
                            content = @Content(
                                    mediaType = "text/plain",
                                    schema = @Schema(type = "string", example = "게시글이 삭제되었습니다.")
                            )
                    )
            }
    )
    @DeleteMapping("/v1/posts/{id}")
    public ApiResponseDto<Object> deletePost(@PathVariable("id") Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails
                                           ) throws AccessDeniedException {
        // 현재 인증된 사용자의 ID를 가져옵니다.
        Long currentUserId = userDetails.getUserId();

        postService.deletePost(postId, currentUserId);

        // 성공적으로 삭제되었을 때 표준적인 응답은 204 No Content 입니다.
        return ApiResponseDto.success("게시글이 삭제되었습니다.");
    }

    // 게시글 수정
    @Operation(
            summary = "게시글 수정",
            description = "게시글 ID와 수정할 데이터를 받아 현재 로그인된 사용자의 게시글을 수정합니다. 자신의 게시글만 수정할 수 있으며, 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            description = "수정할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 게시글의 제목, 내용, 이미지 정보를 담은 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostCreateRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "게시글 수정 성공. 일반적으로 수정(PUT)은 200 OK 또는 204 No Content를 반환하지만, 현재 로직은 201 Created를 반환합니다.",
                            headers = {
                                    @Header(
                                            name = "Location",
                                            description = "수정된 게시글의 URI (예: /v1/posts/42)",
                                            schema = @Schema(type = "string")
                                    )
                            },
                            content = @Content // 성공 시 응답 본문은 없음
                    )
            }
    )
    @PutMapping("/v1/posts/{id}")
    public ApiResponseDto<Object> updatePost(@PathVariable("id") Long postId,
                                           @RequestBody PostCreateRequestDto requestDto,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long currentUserId = userDetails.getUserId();
        postService.updatePost(postId, requestDto, currentUserId);

        return ApiResponseDto.success("게시글이 수정되었습니다.");
    }

    // 게시글 좋아요 추가
    @Operation(
            summary = "게시글 좋아요 추가",
            description = "게시글 ID를 사용하여 현재 로그인된 사용자가 해당 게시글에 '좋아요'를 추가합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            description = "좋아요를 추가할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "좋아요 추가 성공",
                            content = @Content // 성공 시 응답 본문은 없음
                    )
            }
    )
    @PostMapping("/v1/posts/{id}/like")
    public ApiResponseDto<Object> likePost(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 현재 로그인한 사용자의 ID를 가져옵니다.
        Long currentUserId = userDetails.getUserId();

        // 서비스 계층에 좋아요 추가 작업을 위임합니다.
        postService.likePost(postId, currentUserId);

        // 성공 시 200 OK 응답을 반환합니다.
        return ApiResponseDto.success("좋아요가 추가되었습니다.");
    }

    // 게시글 좋아요 취소
    @Operation(
            summary = "게시글 좋아요 취소",
            description = "게시글 ID를 사용하여 현재 로그인된 사용자가 해당 게시글에 누른 '좋아요'를 취소합니다. 요청 헤더에 Access Token이 반드시 필요합니다.",
            security = { @SecurityRequirement(name = "accessTokenAuth") },
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            description = "좋아요를 취소할 게시글의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "좋아요 취소 성공. 응답 본문은 없습니다.",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/v1/posts/{id}/like")
    public ApiResponseDto<Object> unlikePost(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        postService.unlikePost(postId, currentUserId);

        // 성공 시 204 No Content 응답을 반환합니다.
        return ApiResponseDto.success("좋아요가 취소되었습니다.");
    }

}
