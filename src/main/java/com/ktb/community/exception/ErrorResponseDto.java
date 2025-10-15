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
}