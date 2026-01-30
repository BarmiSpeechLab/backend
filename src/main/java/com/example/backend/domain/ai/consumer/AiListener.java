package com.example.backend.domain.ai.consumer;

import com.example.backend.domain.ai.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiListener {

    // private final FeedbackRepository feedbackRepository; // 나중에 DB 업데이트할 때 필요
    private final AnalysisService analysisService;

    /**
     * @RabbitListener: ai.result 큐를 계속 감시합니다
     * 메시지가 들어오면 여기서 큐를 가져옵니다
     */
    @RabbitListener(queues = "pron_result")
    public void receivePronResult(Map<String, Object> rawData) {
        log.info("[IPA 분석 데이터 수신] 원본 데이터: {}", rawData);
        analysisService.saveResult((String) rawData.get("taskId"), "PRON", rawData);
    }

    @RabbitListener(queues = "inton_result")
    public void receiveIntonResult(Map<String, Object> rawData) {
        log.info("[Intonation 분석 데이터 수신] 원본 데이터: {}", rawData);
        analysisService.saveResult((String) rawData.get("taskId"), "INTON", rawData);

    }

    @RabbitListener(queues = "llm_result")
    public void receiveLLMResult(Map<String, Object> rawData) {
        log.info("[LLM Feedback 데이터 수신] 원본 데이터: {}", rawData);
        analysisService.saveResult((String) rawData.get("taskId"), "LLM", rawData);
    }

}
