package com.ktb.community.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD_REQUEST: 잘못된 요청
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E-C001", "입력 값이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "E-C002", "비밀번호가 일치하지 않습니다."),

    // 401 UNAUTHORIZED: 인증되지 않은 사용자
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "E-AU001", "인증되지 않은  사용자입니다. 로그인이 필요합니다."),

    // 403 FORBIDDEN: 접근 권한 없음
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "E-AU002", "접근 권한이 없습니다."),

    // 404 NOT_FOUND: 리소스를 찾을 수 없음
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "E-M001", "해당 회원을 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "E-P001", "해당 게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E-C001", "해당 댓글을 찾을 수 없습니다."),

    // 409 CONFLICT: 리소스 충돌 (중복)
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "E-M002", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATION(HttpStatus.CONFLICT, "E-M003", "이미 사용 중인 닉네임입니다."),
    LIKE_DUPLICATION(HttpStatus.CONFLICT, "E-M004", "이미 좋아요를 누른 게시물입니다."),
    UNLIKE_DUPLICATION(HttpStatus.CONFLICT, "E-M004", "좋아요가 없는 게시물입니다."),
    // 500 INTERNAL_SERVER_ERROR: 서버 내부 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E-S001", "서버에 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
