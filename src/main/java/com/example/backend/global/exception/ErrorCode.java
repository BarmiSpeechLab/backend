package com.example.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // =================================================================
    // 🌍 Global / Common (공통 에러)
    // =================================================================
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 40001, "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, 40002, "입력 값의 타입이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 40501, "허용되지 않는 HTTP 메서드입니다."),

    // =================================================================
    // 🔐 Auth & JWT (인증/인가)
    // =================================================================
    // 401: 인증 실패
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 40101, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 40102, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 40103, "유효하지 않은 리프레시 토큰입니다."),
    MISSING_AUTH_HEADER(HttpStatus.UNAUTHORIZED, 40104, "Authorization 헤더가 없거나 잘못되었습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 40105, "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN_USER(HttpStatus.UNAUTHORIZED, 40106, "유효하지 않은 유저의 토큰입니다"),

    // 403: 권한 없음 (인증은 성공했지만 접근 불가)
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 40301, "접근 권한이 없습니다."),
    TUTOR_ONLY_ACCESS(HttpStatus.FORBIDDEN, 40302, "튜터만 접근 가능한 기능입니다."),

    // =================================================================
    // 👤 User (회원)
    // =================================================================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "해당 사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 40901, "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, 40902, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_ID(HttpStatus.CONFLICT, 40902, "유저 ID가 중복되었습니다."),

    // =================================================================
    // 📚 Curriculum & Practice (학습 & 발음 연습)
    // =================================================================
    EXPRESSION_NOT_FOUND(HttpStatus.NOT_FOUND, 40410, "해당 학습 표현(단어/문장)을 찾을 수 없습니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 42901, "일일 발음 분석 횟수 제한을 초과했습니다."),
    // 파일 업로드 관련
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 50010, "파일 업로드에 실패했습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, 40010, "지원하지 않는 파일 형식입니다. (mp4, webm, wav만 가능)"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, 40011, "파일 크기가 너무 큽니다."),

    // =================================================================
    // 🎤 Audio Analysis (음성 분석 품질 이슈)
    // =================================================================
    // 목소리가 너무 작을 때
    AUDIO_VOLUME_TOO_LOW(HttpStatus.BAD_REQUEST, 40012, "목소리가 너무 작습니다. 마이크를 가까이 대고 다시 시도해주세요."),
    // 주변 소음이 너무 심할 때
    AUDIO_NOISE_DETECTED(HttpStatus.BAD_REQUEST, 40013, "주변 소음이 너무 심합니다. 조용한 곳에서 다시 녹음해주세요."),
    // 녹음은 됐는데 말이 없을 때 (묵음)
    AUDIO_SILENCE_DETECTED(HttpStatus.BAD_REQUEST, 40014, "목소리가 감지되지 않았습니다. 다시 녹음해주세요."),

    // =================================================================
    // 📹 Tutoring & WebRTC (화상 튜터링)
    // =================================================================
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 40420, "해당 튜터링 방을 찾을 수 없습니다."),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND,40430, "해당 수업을 찾을 수 없습니다."),
    ROOM_IS_FULL(HttpStatus.CONFLICT, 40920, "방의 정원이 초과되었습니다."),
    ALREADY_IN_ROOM(HttpStatus.CONFLICT, 40921, "이미 방에 참여 중입니다."),
    ROOM_CLOSED(HttpStatus.BAD_REQUEST, 40020, "이미 종료된 튜터링 방입니다."),
    OPENVIDU_HTTP_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000,"화상 회의 서버 연결에 실패했습니다."),
    OPENVIDU_CLIENT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,50010, "화상 회의 클라이언트 설정 오류입니다."),

    // =================================================================
    // 🤖 AI Integration (FastAPI 연동)
    // =================================================================
    AI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, 50201, "AI 분석 서버와 통신 중 오류가 발생했습니다."),
    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 50020, "발음 분석에 실패했습니다. 다시 시도해주세요."),

    JSON_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50030, "JSON 파싱 에러입니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
