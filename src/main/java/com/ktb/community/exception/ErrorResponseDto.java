package com.ktb.community.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
public class ErrorResponseDto {

    private final int status;
    private final String code;
    private final String message;

    public static ResponseEntity<ErrorResponseDto> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.builder()
                        .status(errorCode.getStatus().value())
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // ✨새로 추가하거나 수정할 메서드✨
    public static ResponseEntity<ErrorResponseDto> toResponseEntity(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponseDto.builder()
                        .status(errorCode.getStatus().value())
                        .code(errorCode.getCode())
                        .code(errorCode.getCode())
                        .message(message) // @Valid에서 가져온 동적 메시지로 설정
                        .build());
    }
}