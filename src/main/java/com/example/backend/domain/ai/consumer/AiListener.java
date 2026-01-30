package com.example.backend.domain.ai.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiListener {

    // private final FeedbackRepository feedbackRepository; // 나중에 DB 업데이트할 때 필요

    /**
     * @RabbitListener: ai.result 큐를 계속 감시합니다
     * 메시지가 들어오면 여기서 큐를 가져옵니다
     */
    @RabbitListener(queues = "pron_result")
    public void receivePronResult(Message rawData) {
        // TODO : parameter 추후에 AnalysisResultDto 형식으로 리팩토링 예정
        String jsonString = new String(rawData.getBody(), StandardCharsets.UTF_8);

        log.info("[IPA 분석 데이터 수신] 원본 데이터: {}", jsonString);

        // TODO: 여기서 DB에 USER_LOGS 저장하는 로직 추가 할 예정
        // resultDto 전처리 -> DB insert
    }

    @RabbitListener(queues = "inton_result")
    public void receiveIntonResult(Message rawData) {
        // TODO : parameter 추후에 AnalysisResultDto 형식으로 리팩토링 예정
        String jsonString = new String(rawData.getBody(), StandardCharsets.UTF_8);

        log.info("[Intonation 분석 데이터 수신] 원본 데이터: {}", jsonString);

        // TODO: 여기서 DB에 USER_LOGS 저장하는 로직 추가 할 예정
        // resultDto 전처리 -> DB insert
    }

    @RabbitListener(queues = "llm_result")
    public void receiveLLMResult(Message rawData) {
        // TODO : parameter 추후에 AnalysisResultDto 형식으로 리팩토링 예정
        String jsonString = new String(rawData.getBody(), StandardCharsets.UTF_8);

        log.info("[LLM Feedback 데이터 수신] 원본 데이터: {}", jsonString);

        // TODO: 여기서 DB에 USER_LOGS 저장하는 로직 추가 할 예정
        // resultDto 전처리 -> DB insert
    }

}
