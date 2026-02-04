package com.example.backend.domain.conversation.consumer;

import com.example.backend.domain.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationListener {

    private final ConversationService conversationService;

    /**
     * @RabbitListener: conversation_result 큐를 계속 감시합니다
     * AI Gateway에서 대화 분석 결과가 도착하면 처리합니다
     */
    @RabbitListener(queues = "conversation_result")
    public void receiveConversationResult(Map<String, Object> rawData) {
        log.info("[대화 분석 데이터 수신] 원본 데이터: {}", rawData);
        conversationService.saveConversationResult((String) rawData.get("taskId"), rawData);
    }
}
