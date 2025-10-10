package com.ktb.community.controller;

import com.ktb.community.dto.request.JoinRequestDto;
import com.ktb.community.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //필터단에서 처리되므로 사시살 동작하지 않음
    @PostMapping("/v1/auth/join")
    public ResponseEntity<String> join(@Valid @RequestBody JoinRequestDto dto) {
        authService.join(dto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }


    @PostMapping("/v1/auth/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response){
        return authService.reissue(request, response);
    }
}
