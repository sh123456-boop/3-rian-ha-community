package com.ktb.community.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [ BusinessException 처리 ]
     * 서비스 로직에서 발생하는 모든 비즈니스 예외를 처리합니다.
     * @param e BusinessException
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException e) {
        log.warn("handleBusinessException: {}", e.getErrorCode().getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        return ErrorResponseDto.toResponseEntity(errorCode);
    }

    /**
     * [ @Valid 유효성 검사 실패 처리 ]
     * @RequestBody, @ModelAttribute 에서 @Valid 어노테이션으로 유효성 검사 실패 시 발생합니다.
     * @param e MethodArgumentNotValidException
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("handleMethodArgumentNotValidException: {}", e.getMessage());
        return ErrorResponseDto.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * [ 나머지 예외 처리 ]
     * 위에서 처리되지 않은 모든 예외를 처리합니다. (최후의 보루)
     * @param e Exception
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        log.error("unhandledException: {}", e.getMessage(), e);
        return ErrorResponseDto.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
