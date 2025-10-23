package com.ktb.community.service;

import com.ktb.community.dto.request.JoinRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {

    // 회원가입
    void join(JoinRequestDto dto);

    // refresh 토큰으로 access 토큰 재발급
    ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response);

}
