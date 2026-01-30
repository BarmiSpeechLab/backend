package com.example.backend.domain.ai.producer;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiClient {
    private final RabbitTemplate rabbitTemplate;

    // 인프라 중심 아키텍처입니다
    // definition.js 파일의 설정을 기반으로 연결합니다
    private final String EXCHANGE_NAME = "ai.exchange";
    private final String ROUTING_KEY = "ai.job";

    public void sendJob(AnalysisRequestDto requestDto) {
        log.info("AI 분석 요청 전송: {}", requestDto);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, requestDto);
    }
}
