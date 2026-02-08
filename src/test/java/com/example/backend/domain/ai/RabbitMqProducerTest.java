package com.example.backend.domain.ai;

import com.example.backend.domain.ai.producer.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class AiClientTest {

    @Autowired
    private AiClient aiClient;

    @Test
    @DisplayName("RabbitMQ 메시지 전송 테스트")
    void testSendJob() {
        // 1. 가짜 데이터 생성
        Map<String, Object> request = new HashMap<>();
        request.put("type", "TEST");
        request.put("file_path", "/app/uploads/test_audio.wav");
        request.put("taskId", "TEST_TASK_123");

        // 2. 메시지 전송
        System.out.println("[테스트] 메시지 전송 시작...");
        aiClient.sendJob(request);
        System.out.println("[테스트] 메시지 전송 요청 끝!");
    }
}
