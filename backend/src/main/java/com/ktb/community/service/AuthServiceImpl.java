package com.ktb.community.service;

import com.ktb.community.dto.request.JoinRequestDto;
import com.ktb.community.entity.RefreshEntity;
import com.ktb.community.entity.Role;
import com.ktb.community.entity.User;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.exception.ErrorCode;
import com.ktb.community.repository.RefreshRepository;
import com.ktb.community.repository.UserRepository;
import com.ktb.community.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.ktb.community.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RefreshRepository refreshRepository;
    private final JWTUtil jwtUtil;

    // 회원가입
    @Transactional
    public void join(JoinRequestDto dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String rePassword = dto.getRePassword();
        String nickname = dto.getNickname();

        //비밀번호 검증 로직
        if(!password.equals(rePassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        boolean isExistEmail = userRepository.existsByEmail(email);
        boolean isExistNickname = userRepository.existsByNickname(nickname);
        if  (isExistEmail) {
            throw new BusinessException(EMAIL_DUPLICATION);
        }
        if (isExistNickname) {
            throw new BusinessException(NICKNAME_DUPLICATION);
        }

        User user = User.builder()
                .nickname(nickname)
                .password(bCryptPasswordEncoder.encode(password))
                .email(email)
                .role(Role.valueOf("USER"))
                .build();
        userRepository.save(user);
    }

    // refresh 토큰으로 access 토큰 재발급
    @Transactional
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {

        // get refresh token
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("refresh")) {
                refresh = cookie.getValue();
            }
        }

        if (refresh == null) {
            throw new BusinessException(UNAUTHORIZED_USER);
        }

        //expired check
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {

            //response status code
            throw  new BusinessException(UNAUTHORIZED_USER);
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(refresh);

        if (!category.equals("refresh")) {

            throw new BusinessException(UNAUTHORIZED_USER);
        }

        // 토큰이 내 토큰 저장소에 등록된 토큰인지 확인
        Boolean exists = refreshRepository.existsByRefresh(refresh);
        if (!exists) {
            throw new BusinessException(UNAUTHORIZED_USER);
        }

        Long Id = jwtUtil.getID(refresh);
        String role = jwtUtil.getRole(refresh);

        //make new JWT
        String newAccess = jwtUtil.createJwt("access", Id, role, 3*600000L); //30분 : 3*600000L
        String newRefresh = jwtUtil.createJwt("refresh", Id, role, 3*86400000L); //3일

        // Refresh 토큰 저장, db에 기존의 refresh 토큰 삭제 후 새 refresh 토큰 저장
        refreshRepository.deleteByRefresh(refresh);
        addRefreshEntity( Id, newRefresh, 3*86400000L);

        ResponseCookie cookie = ResponseCookie.from("refresh", newRefresh)
                .path("/")
                .maxAge(24 * 60 * 60 * 3)
                .httpOnly(true)
                .secure(true)
                .sameSite("None") // http 환경의 cross-site 통신을 위해 "Lax"로 설정
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        //response
        response.setHeader("access", newAccess);
        //response.addCookie(createCookie("refresh", newRefresh));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        //cookie.setSecure(true);
        //cookie.setPath("/");
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
