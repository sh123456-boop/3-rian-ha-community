package com.ktb.community.dto;

import lombok.Getter;

@Getter
public class ApiResponseDto<T> {

    // 1. 상태 (SUCCESS, FAIL, ERROR)
    private final String status;
    // 2. 우리 시스템의 고유 코드
    private final String code;
    // 3. 응답 메시지
    private final String message;
    // 4. 실제 데이터
    private final T data;

    // 성공 시 사용하는 생성자
    private ApiResponseDto(String status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // [정적 팩토리 메서드]

    // 1. 성공 응답 (데이터만 포함)
    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>("SUCCESS", "S-001", "요청에 성공하였습니다.", data);
    }

    // 2. 성공 응답 (코드, 메시지 커스텀)
    public static <T> ApiResponseDto<T> success() {
        return new ApiResponseDto<>("SUCCESS", "S-001", "요청에 성공하였습니다.", null);
    }

    public static <T> ApiResponseDto<T> success(String message) {
        return new ApiResponseDto<>("SUCCESS", "S-001", message, null);
    }



}