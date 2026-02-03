package com.example.backend.global.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApiLogService {

    @Async("loggingTaskExecutor")
    public void saveApiLog(String method, String url, String params, String response, long duration) {
        // DB에 저장하고 싶다면 LogRepository.save(...) 호출
        // 여기서는 가독성 좋게 콘솔 출력 예시만 작성합니다.
        log.info("\n[API Request Log]\n" +
                        "Method: {}\n" +
                        "URL: {}\n" +
                        "Parameters: {}\n" +
                        "Response: {}\n" +
                        "Duration: {}ms",
                method, url, params, response, duration);
    }
}
