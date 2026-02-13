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

    private final AnalysisService analysisService;

    /**
     * @RabbitListener: pron_result 큐를 계속 감시합니다
     * 메시지가 들어오면 여기서 큐를 가져옵니다
     */
    @RabbitListener(queues = "pron_result")
    public void receivePronResult(Map<String, Object> rawData) {
        log.info("========================================");
        log.info("[RabbitMQ] pron_result 큐에서 메시지 수신");
        log.info("[원본 데이터] {}", rawData);
        
        try {
            String taskId = extractTaskId(rawData);
            log.info("[TaskID 추출] taskId={}", taskId);
            
            if (taskId == null || taskId.isEmpty()) {
                log.error("[ERROR] TaskID가 null 또는 빈 문자열입니다. 메시지 무시됨");
                return;
            }
            
            analysisService.saveResult(taskId, "PRON", rawData);
            log.info("[IPA 분석 저장 완료] taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("[ERROR] pron_result 처리 중 예외 발생", e);
            log.error("[예외 상세] 메시지: {}, 원본 데이터: {}", e.getMessage(), rawData);
        }
        log.info("========================================");
    }

    @RabbitListener(queues = "inton_result")
    public void receiveIntonResult(Map<String, Object> rawData) {
        log.info("========================================");
        log.info("[RabbitMQ] inton_result 큐에서 메시지 수신");
        log.info("[원본 데이터] {}", rawData);
        
        try {
            String taskId = extractTaskId(rawData);
            log.info("[TaskID 추출] taskId={}", taskId);
            
            if (taskId == null || taskId.isEmpty()) {
                log.error("[ERROR] TaskID가 null 또는 빈 문자열입니다. 메시지 무시됨");
                return;
            }
            
            analysisService.saveResult(taskId, "INTON", rawData);
            log.info("[Intonation 분석 저장 완료] taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("[ERROR] inton_result 처리 중 예외 발생", e);
            log.error("[예외 상세] 메시지: {}, 원본 데이터: {}", e.getMessage(), rawData);
        }
        log.info("========================================");
    }

    @RabbitListener(queues = "llm_result")
    public void receiveLLMResult(Map<String, Object> rawData) {
        log.info("========================================");
        log.info("[RabbitMQ] llm_result 큐에서 메시지 수신");
        log.info("[원본 데이터] {}", rawData);
        
        try {
            String taskId = extractTaskId(rawData);
            log.info("[TaskID 추출] taskId={}", taskId);
            
            if (taskId == null || taskId.isEmpty()) {
                log.error("[ERROR] TaskID가 null 또는 빈 문자열입니다. 메시지 무시됨");
                return;
            }
            
            analysisService.saveResult(taskId, "LLM", rawData);
            log.info("[LLM Feedback 저장 완료] taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("[ERROR] llm_result 처리 중 예외 발생", e);
            log.error("[예외 상세] 메시지: {}, 원본 데이터: {}", e.getMessage(), rawData);
        }
        log.info("========================================");
    }

    @RabbitListener(queues = "error_result")
    public void receiveErrorResult(Map<String, Object> rawData) {
        log.info("========================================");
        log.error("[RabbitMQ] error_result 큐에서 에러 메시지 수신");
        log.error("[에러 데이터] {}", rawData);
        
        try {
            String taskId = extractTaskId(rawData);
            log.error("[TaskID 추출] taskId={}", taskId);
            
            if (taskId == null || taskId.isEmpty()) {
                log.error("[ERROR] TaskID가 null 또는 빈 문자열입니다. 메시지 무시됨");
                return;
            }
            
            analysisService.saveResult(taskId, "ERROR", rawData);
            log.error("[에러 결과 저장 완료] taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("[ERROR] error_result 처리 중 예외 발생", e);
            log.error("[예외 상세] 메시지: {}, 원본 데이터: {}", e.getMessage(), rawData);
        }
        log.info("========================================");
    }

    /**
     * taskId 추출 유틸 메서드 (다양한 케이스 대응)
     */
    private String extractTaskId(Map<String, Object> rawData) {
        if (rawData == null) {
            log.error("[TaskID 추출 실패] rawData가 null입니다");
            return null;
        }

        // 1. taskId 키로 직접 조회
        Object taskIdObj = rawData.get("taskId");
        if (taskIdObj != null) {
            String taskId = String.valueOf(taskIdObj);
            log.debug("[TaskID 추출] 'taskId' 키에서 직접 추출: {}", taskId);
            return taskId;
        }

        // 2. task_id (snake_case) 키로 조회
        taskIdObj = rawData.get("task_id");
        if (taskIdObj != null) {
            String taskId = String.valueOf(taskIdObj);
            log.debug("[TaskID 추출] 'task_id' 키에서 추출: {}", taskId);
            return taskId;
        }

        // 3. 모든 키 출력 (디버깅용)
        log.error("[TaskID 추출 실패] taskId 키를 찾을 수 없습니다");
        log.error("[사용 가능한 키 목록] {}", rawData.keySet());
        
        return null;
    }
}
