package com.example.backend.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
// 커스텀 예외 처리를 정의하는 클래스입니다

    private final ErrorCode errorCode;

    // 예외처리할 때 에러코드를 파라미터로 넣어서
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
