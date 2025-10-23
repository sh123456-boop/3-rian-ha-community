package com.ktb.community.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.dto.request.LoginRequestDto;
import com.ktb.community.entity.RefreshEntity;
import com.ktb.community.repository.RefreshRepository;
import com.ktb.community.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


import java.io.IOException;
import java.util.Date;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    // JSON 파싱을 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RefreshRepository refreshRepository;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
        // 로그인 요청을 처리할 URL을 설정합니다.
        setFilterProcessesUrl("/v1/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 1. 요청 본문(request body)의 JSON 데이터를 DTO로 변환
            LoginRequestDto loginRequestDto = objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);

            String email = loginRequestDto.getEmail();
            String password = loginRequestDto.getPassword();

            // 2. DTO에서 추출한 email, password로 토큰 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);

            // 3. 생성된 토큰으로 AuthenticationManager에게 인증 요청
            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new RuntimeException("로그인 데이터 파싱 중 오류가 발생했습니다.", e);
        }
    }

    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {
        // 1. Authentication 객체에서 CustomUserDetails 객체 꺼내기
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        // 2. CustomUserDetails에서 user_id와 role 가져오기
        Long userId = customUserDetails.getUserId();
        String role = customUserDetails.getAuthorities().iterator().next().getAuthority();

        // 3. 토큰 생성
        String access = jwtUtil.createJwt("access", userId, role, 3*600000L); //30분 3*600000L  // 1분 60000L
        String refresh = jwtUtil.createJwt("refresh", userId, role, 3*86400000L); //3일

        // 4. Refresh 토큰 저장
        addRefreshEntity(userId, refresh,3*86400000L );

        // 5. 응답 설정
       ResponseCookie cookie = ResponseCookie.from("refresh", refresh)
                .path("/")
                .maxAge(24 * 60 * 60 * 3)
                .httpOnly(true)
               .secure(true)
                .sameSite("None") // http 환경의 cross-site 통신을 위해 "Lax"로 설정
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        response.setHeader("access", access); // access 토큰은 헤더에 발급 후 로컬 스토리지에 저장 (csrf 공격 방어)
        //response.addCookie(createCookie("refresh", refresh)); // refresh 토큰은 쿠키에 발급 (XSS 공격 방어)
        response.setStatus(HttpStatus.OK.value());
    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        // 응답 상태코드를 401 (Unauthorized)로 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 실패 원인(Exception)에 따라 다른 에러 메시지 설정
        String errorMessage;
        // '해당 이메일은 등록되지 않았습니다' 예외는 InternalAuthenticationServiceException 으로 래핑됩니다.
        if (failed instanceof org.springframework.security.authentication.InternalAuthenticationServiceException) {
            errorMessage = "로그인에 실패하였습니다.";
        } else if (failed instanceof org.springframework.security.authentication.BadCredentialsException) {
            errorMessage = "비밀번호가 일치하지 않습니다.";
        } else {
            errorMessage = "로그인에 실패하였습니다.";
        }

        // ErrorResponseDto와 유사한 형식의 JSON을 직접 생성하여 응답
        String jsonResponse = String.format(
                "{\"status\": %d, \"code\": \"LOGIN_FAILED\", \"message\": \"%s\"}",
                HttpServletResponse.SC_UNAUTHORIZED,
                errorMessage
        );

        response.getWriter().write(jsonResponse);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

    private void addRefreshEntity(Long id, String refresh, Long expiredMs) {

        Date date = new Date(System.currentTimeMillis() + expiredMs);

        RefreshEntity refreshEntity = RefreshEntity.builder()
                .userId(id)
                .refresh(refresh)
                .expiration(date.toString())
                .build();

        refreshRepository.save(refreshEntity);
    }
}