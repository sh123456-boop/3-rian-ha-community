package com.ktb.community.controller;

import com.ktb.community.dto.ApiResponseDto;
import com.ktb.community.dto.request.JoinRequestDto;
import com.ktb.community.dto.request.LoginRequestDto;
import com.ktb.community.service.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "인증 도메인 API")
public class AuthController {

    private final AuthServiceImpl authService;


    // 회원가입
    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 비밀번호(확인용), 닉네임을 DTO로 보내면 회원가입시켜주는 로직",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원가입 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JoinRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200", // 또는 "201"
                            description = "회원가입 성공",
                            content = @Content(
                                    mediaType = "text/plain",
                                    schema = @Schema(type = "string", example = "회원가입이 완료되었습니다")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "회원가입 실패: 입력값 유효성 검사 오류",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "회원가입 실패: 이미 가입된 이메일",
                            content = @Content
                    )
            }
    )
    @PostMapping("/v1/auth/join")
    public ApiResponseDto<Object> join(@Valid @RequestBody JoinRequestDto dto) {
        authService.join(dto);
        return ApiResponseDto.success("회원가입이 완료되었습니다.");
    }

    // Access_Token 토큰 재발급
    @Operation(
            summary = "Access Token 재발급",
            description = "Cookie에 담긴 Refresh Token을 검증하여 새로운 Access Token과 Refresh Token을 발급합니다.",
            parameters = {
                    @Parameter(
                            name = "refresh", // 실제 쿠키의 이름
                            in = ParameterIn.COOKIE,
                            description = "Access Token 재발급을 위한 Refresh Token",
                            required = true,
                            schema = @Schema(type = "string")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "토큰 재발급 성공",
                            headers = {
                                    @Header(
                                            name = "access 토큰 발급(헤더)",
                                            description = "access : (토큰값) ",
                                            schema = @Schema(type = "string")
                                    ),
                                    @Header(
                                            name = "refresh 토큰 발급(쿠키)",
                                            description = "refresh : (토큰값)",
                                            schema = @Schema(type = "string")
                                    )
                            },
                            content = @Content // 성공 시 응답 본문은 없으므로 비워둡니다.
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = " 토큰 재발급 실패: 입력값 유효성 검사 오류",
                            content = @Content
                    )
            }
    )
    @PostMapping("/v1/auth/reissue")
    public ApiResponseDto<Object> reissue(HttpServletRequest request, HttpServletResponse response){
        authService.reissue(request, response);
        return ApiResponseDto.success("토큰이 재발급되었습니다.");
    }

    // 로그인은 필터 단에서 처리되므로 해당 로직은 수행하지 않음.
    @Operation(
            summary = "로그인",
            description = "이메일, 비밀번호를 이용해 로그인하는 로직",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인 JSON Body 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            headers = {
                                    @Header(
                                            name = "access 토큰 발급(헤더)",
                                            description = "access : (토큰값) ",
                                            schema = @Schema(type = "string")
                                    ),
                                    @Header(
                                            name = "refresh 토큰 발급(쿠키)",
                                            description = "refresh : (토큰값)",
                                            schema = @Schema(type = "string")
                                    )
                            },
                            content = @Content // 성공 시 응답 본문은 없으므로 비워둡니다.
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "로그인 실패: 입력값 유효성 검사 오류",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "로그인 실패: 이메일 또는 비밀번호 불일치",
                            content = @Content
                    )
            }
    )
    @PostMapping("/v1/auth/login")
    public void login(){}


    // 로그아웃은 필터 단에서 처리되므로 해당 로직은 수행하지 않음.
    @Operation(
            summary = "로그아웃",
            description = "Cookie에 담긴 Refresh Token을 만료시켜 로그아웃을 처리합니다.",
            parameters = {
                    @Parameter(
                            name = "refresh",
                            in = ParameterIn.COOKIE,
                            description = "로그아웃할 사용자의 Refresh Token",
                            required = true,
                            schema = @Schema(type = "string")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그아웃 성공",
                            headers = {
                                    @Header(
                                            name = "Set-Cookie",
                                            description = "refresh token 값을 비우고 Max-Age=0으로 설정",
                                            schema = @Schema(type = "null")
                                    )
                            },
                            content = @Content // 로그아웃 성공 시 응답 본문은 없음
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "로그아웃 실패: 요청에 유효한 Refresh Token 쿠키가 없음",
                            content = @Content
                    )
            }
    )
    @PostMapping("/v1/auth/logout")
    public void logout(){}
}
