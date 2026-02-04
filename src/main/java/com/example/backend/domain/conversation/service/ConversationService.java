package com.example.backend.domain.conversation.service;

import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.domain.conversation.dto.ConversationAnalysisDetail;
import com.example.backend.domain.conversation.dto.ConversationAnalysisResult;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import com.example.backend.global.infra.file.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {
    
    private final AiClient aiClient;
    private final FileService fileService;
    private final CacheManager cacheManager;
    private final CurriculumRepository curriculumRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 대화 분석 요청 서비스 메서드
     * Spring -> AI Gateway (RabbitMQ)
     */
    public String requestConversationAnalysis(MultipartFile file, String prevTurn, String theme) {
        // 파일 저장
        String savedFileName = fileService.saveFile(file);
        // Task ID 생성
        String taskId = "CONV_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 대화 요청 데이터 준비 (FastAPI snake_case 맞춤)
        Map<String, Object> analysisRequest = new HashMap<>();
        analysisRequest.put("prev_turn", prevTurn);  // ✅ snake_case
        analysisRequest.put("theme", theme);

        Map<String, Object> request = new HashMap<>();
        request.put("file_path", savedFileName);
        request.put("task_id", taskId);
        request.put("type", "conversation");  // 대화 분석 타입
        request.put("analysis_request", analysisRequest);  // ✅ snake_case

        // AI 서버로 전송 (conversation.jobs 큐)
        aiClient.sendConversationJob(request);
        log.info("대화 분석 요청 데이터: {}", request);
        
        return taskId;
    }

    /**
     * AI 결과 저장 서비스 메서드
     * AI Gateway -> Spring (RabbitMQ conversation_result 큐)
     */
    public void saveConversationResult(String taskId, Map<String, Object> rawData) {
        if (taskId == null) {
            log.error("TaskId가 누락된 대화 분석 결과가 수신되었습니다.");
            return;
        }

        // 캐시 가져오기
        org.springframework.cache.Cache springCache = cacheManager.getCache("conversation_results");
        if (springCache == null) {
            log.error("conversation_results 캐시를 찾을 수 없습니다.");
            return;
        }

        // Caffeine Cache로 형 변환
        Cache<Object, Object> caffeineCache = (Cache<Object, Object>) springCache.getNativeCache();

        // 동시성 문제 해결을 위해 compute() 사용
        caffeineCache.asMap().compute(taskId, (key, existingValue) -> {
            ConversationAnalysisResult result = (ConversationAnalysisResult) existingValue;
            if (result == null) {
                result = new ConversationAnalysisResult();
                result.setTaskId(taskId);
            }

            // analysisResult 필드 파싱하여 ConversationAnalysisDetail로 변환
            try {
                Object analysisResultObj = rawData.get("analysisResult");
                if (analysisResultObj != null) {
                    ConversationAnalysisDetail detail = objectMapper.convertValue(
                        analysisResultObj, 
                        ConversationAnalysisDetail.class
                    );
                    result.setAnalysisResult(detail);
                    result.setStatus("SUCCESS");
                    log.info("대화 분석 결과 파싱 성공: {}", detail);
                } else {
                    log.warn("analysisResult 필드가 null입니다.");
                }
            } catch (Exception e) {
                log.error("대화 분석 결과 파싱 실패: {}", e.getMessage());
                result.markAsError();
            }

            return result;
        });

        log.info("대화 분석 결과 저장 완료. TaskId: {}", taskId);
    }

    /**
     * 조회 메서드
     * 프론트엔드 Polling 요청 처리
     */
    public ConversationAnalysisResult getResult(String taskId) {
        // 캐시 가져오기
        org.springframework.cache.Cache cache = cacheManager.getCache("conversation_results");
        if (cache == null) {
            return createProcessingResult(taskId);
     }

     // TaskId로 데이터 조회
     ConversationAnalysisResult result = cache.get(taskId, ConversationAnalysisResult.class);
     if (result == null) {
     return createProcessingResult(taskId);
     }

     if ("ERROR".equals(result.getStatus())) {
     cache.evict(taskId);
     return result;
     }

     // 분석 완료 시 캐시 삭제
     if (result.isComplete()) {
     cache.evict(taskId);
     result.setStatus("SUCCESS");
     }

     return result;
     }

     /**
     * PROCESSING 상태의 빈 결과 객체 생성
     */
    private ConversationAnalysisResult createProcessingResult(String taskId) {
        ConversationAnalysisResult result = new ConversationAnalysisResult();
        result.setTaskId(taskId);
        result.setStatus("PROCESSING");
        return result;
    }
}
