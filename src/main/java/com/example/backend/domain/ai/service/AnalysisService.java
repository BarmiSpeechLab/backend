package com.example.backend.domain.ai.service;

import com.example.backend.domain.ai.dto.IntegratedAnalysisResult;
import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.report.entity.DailyStudyLog;
import com.example.backend.domain.report.repository.DailyStudyLogRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import com.example.backend.global.infra.file.FileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {
    private final AiClient aiClient;
    private final FileService fileService;
    private final CacheManager cacheManager;
    private final CurriculumRepository curriculumRepository;
    private final CurriculumStatsRepository curriculumStatsRepository;
    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 분석 요청 서비스 메서드
    // request -> AI
    public String requestAnalysis(MultipartFile file, Long curriculumId) {
        // 파일저장
        String savedFileName = fileService.saveFile(file);
        // id로 커리큘럼 찾고 npe 처리
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(()->new CustomException(ErrorCode.EXPRESSION_NOT_FOUND));
        // task id 생성
        String taskId = "REQ_" + UUID.randomUUID().toString().substring(0, 8);
        // 2. cData가 Map이므로 바로 "analysis" 키의 값을 꺼냅니다.
        // SQL 구조상 {"analysis": {...}} 형태이므로 get("analysis") 결과는 다시 Map이 됩니다.
        Map<String, Object> cDataMap = curriculum.getCData();
        Object analysisData = cDataMap.get("analysis");

        Map<String, Object> request = new HashMap<>();
        request.put("file_path", savedFileName);
        request.put("taskId", taskId);
        request.put("type", curriculum.getType());

        // AI 서버 규격에 맞춰 "analysisRequest"라는 키에 실제 데이터 주입
        request.put("analysisRequest", analysisData);

        // 4. 전송
        aiClient.sendJob(request);
        log.info("AI 분석 요청 데이터: {}", request);
        return taskId;
    }

    // AI 결과 통합 저장 서비스 메서드
    // AI -> Spring Cache
    public void saveResult(String taskId, String type, Map<String, Object> rawData) {
        log.info("========================================");
        log.info("[AnalysisService.saveResult 호출]");
        log.info("[TaskID] {}", taskId);
        log.info("[Type] {}", type);
        log.info("[RawData] {}", rawData);
        
        if (taskId == null) {
            log.error("[저장 실패] TaskId가 누락된 결과가 수신되었습니다. Type: {}", type);
            return;
        }
        
        try {
            // 1. 스프링 캐시 매니저에서 껍데기(Spring Cache) 가져오기
            org.springframework.cache.Cache springCache = cacheManager.getCache("analysis_results");
            if (springCache == null) {
                log.error("[저장 실패] 캐시 'analysis_results'를 찾을 수 없습니다");
                return;
            }
            log.debug("[캐시 조회 성공] analysis_results 캐시 획득");

            // 2. Caffeine Cache로 형 변환
            Cache<Object, Object> caffeineCache = (Cache<Object, Object>) springCache.getNativeCache();
            log.debug("[Caffeine Cache 변환 완료]");

            // 3. 동시성 문제 해결을 위해 asMap().compute() 사용
            caffeineCache.asMap().compute(taskId, (key, existingValue) -> {
                log.debug("[캐시 업데이트 시작] taskId={}, 기존값 존재={}", taskId, existingValue != null);

                // 기존 값이 있으면 쓰고, 없으면(null) 새로 만듦
                IntegratedAnalysisResult report = (IntegratedAnalysisResult) existingValue;
                if (report == null) {
                    log.info("[새 리포트 생성] taskId={}", taskId);
                    report = new IntegratedAnalysisResult();
                    report.setTaskId(taskId);
                } else {
                    log.info("[기존 리포트 업데이트] taskId={}", taskId);
                    logCurrentReportStatus(report);
                }

                // 4. 타입에 따라 "해당 칸"에만 데이터 채우기
                switch (type) {
                    case "PRON" -> {
                        report.setPronunciation(rawData);
                        log.info("[PRON 데이터 저장] taskId={}", taskId);
                    }
                    case "INTON" -> {
                        report.setIntonations(rawData);
                        log.info("[INTON 데이터 저장] taskId={}", taskId);
                    }
                    case "LLM" -> {
                        report.setLlmFeedback(rawData);
                        log.info("[LLM 데이터 저장] taskId={}", taskId);
                    }
                    case "ERROR" -> {
                        report.setStatus("ERROR");
                        report.setError(String.valueOf(rawData.get("error")));
                        log.error("[ERROR 상태 저장] taskId={}, error={}", taskId, rawData.get("error"));
                    }
                }

                // 업데이트 후 상태 로깅
                logCurrentReportStatus(report);
                return report; // 업데이트된 객체 리턴 (캐시에 자동 저장됨)
            });

            log.info("[데이터 병합 완료] Type={}, TaskId={}", type, taskId);
            
        } catch (Exception e) {
            log.error("[저장 중 예외 발생] taskId={}, type={}", taskId, type, e);
            log.error("[예외 상세] 메시지={}, rawData={}", e.getMessage(), rawData);
        }
        
        log.info("========================================");
    }
    
    /**
     * 현재 리포트 상태를 로깅하는 헬퍼 메서드
     */
    private void logCurrentReportStatus(IntegratedAnalysisResult report) {
        log.debug("[리포트 상태] taskId={}, PRON={}, INTON={}, LLM={}, Status={}", 
            report.getTaskId(),
            report.getPronunciation() != null ? "완료" : "대기중",
            report.getIntonations() != null ? "완료" : "대기중",
            report.getLlmFeedback() != null ? "완료" : "대기중",
            report.getStatus()
        );
    }
    // 조회 메서드
    @Transactional
    public IntegratedAnalysisResult getResult(Long userId, String taskId, Long curriculumId) {
        log.info("[AnalysisService.getResult 호출] userId={}, taskId={}, curriculumId={}", userId, taskId, curriculumId);
        
        // 1. 캐시 가져오기
        org.springframework.cache.Cache cache = cacheManager.getCache("analysis_results");
        if (cache == null) {
            log.warn("[캐시 없음] analysis_results 캐시를 찾을 수 없습니다. PROCESSING 반환");
            return createProcessingResult(taskId);
        }

        // 2. TaskId로 데이터 조회
        IntegratedAnalysisResult result = cache.get(taskId, IntegratedAnalysisResult.class);
        if (result == null) {
            log.info("[캐시 미스] taskId={}에 대한 결과가 아직 캐시에 없습니다. PROCESSING 반환", taskId);
            return createProcessingResult(taskId);
        }
        
        log.info("[캐시 히트] taskId={} 데이터 조회 성공", taskId);
        logCurrentReportStatus(result);
        
        if ("ERROR".equals(result.getStatus())) {
            log.error("[에러 상태 감지] taskId={}, error={}", taskId, result.getError());
            cache.evict(taskId);
            log.info("[캐시 삭제] 에러 결과 캐시 제거 완료");
            return result;
        }
        
        // 분석 완료 체크 (세 가지 결과가 모두 도착했는지)
        boolean isComplete = isAnalysisComplete(result);
        log.info("[완료 상태 체크] taskId={}, 완료={}", taskId, isComplete);
        
        if (isComplete) {
            log.info("[분석 완료] taskId={} - DB 저장 시작", taskId);
            
            try {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                Curriculum curriculum = curriculumRepository.findById(curriculumId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EXPRESSION_NOT_FOUND));
                
                log.info("[DB 저장] user={}, curriculum={}", user.getNickname(), curriculum.getId());
                
                // DB 업데이트
                saveToDatabase(user, result, curriculum);
                
                cache.evict(taskId);
                log.info("[캐시 삭제] DB 저장 완료 후 캐시 제거");
                
                result.setStatus("SUCCESS");
                log.info("[최종 상태] taskId={} - SUCCESS", taskId);
                
            } catch (Exception e) {
                log.error("[DB 저장 실패] taskId={}", taskId, e);
                throw e;
            }
        } else {
            log.info("[분석 진행중] taskId={} - 일부 결과만 도착함", taskId);
        }
        
        return result;
    }
    
    /**
     * PROCESSING 상태의 빈 결과 객체 생성
     */
    private IntegratedAnalysisResult createProcessingResult(String taskId) {
        IntegratedAnalysisResult result = new IntegratedAnalysisResult();
        result.setTaskId(taskId);
        result.setStatus("PROCESSING");
        log.debug("[PROCESSING 객체 생성] taskId={}", taskId);
        return result;
    }
    private boolean isAnalysisComplete(IntegratedAnalysisResult result) {
        return result.getPronunciation() != null
                && result.getIntonations() != null
                && result.getLlmFeedback() != null;
    }

    // DB 저장 로직 분리
    @Transactional
    public void saveToDatabase(User user, IntegratedAnalysisResult result, Curriculum curriculum) {
        /// 1. 점수 추출 (안전한 타입 변환 로직 적용)
        // Map에서 "score"를 가져오되, Integer/Double/String 모든 경우를 대비합니다.
        int score = 0;
        if (result.getPronunciation() instanceof Map<?, ?> pronMap) {
            Object scoreObj = pronMap.get("score");
            score = convertToInteger(scoreObj);
        }
        // 2. CurriculumStats (커리큘럼별 최고기록/완료여부) 업데이트
        CurriculumStats stats = curriculumStatsRepository.findByUserAndCurriculumId(user, curriculum.getId())
                .orElseGet(() -> CurriculumStats
                        .builder()
                        .curriculum(curriculum)
                        .user(user)
                        .score(0)   // 초기 점수 세팅
                        .build());

        stats.updateScore(score);   // 기존 점수보다 높으면 갱신하는 로직
        curriculumStatsRepository.save(stats);

        // 3. DailyStudyLog (일일 학습량) 업데이트
        DailyStudyLog todayLog = dailyStudyLogRepository.findByUserAndDate(user, LocalDate.now())
                .orElseGet(() -> {
                    DailyStudyLog newLog = new DailyStudyLog(user, LocalDate.now());
                    return dailyStudyLogRepository.save(newLog); // 신규 생성 시 즉시 저장
                });
        todayLog.increaseFeedbackCount();
        dailyStudyLogRepository.save(todayLog); // @Transactional이 있으므로 dirty checking에 의해 저절로 업데이트 되긴 함
        log.info("DB 저장 완료: User={}, Score={}, Curriculum={}", user.getNickname(), score, curriculum.getId());
    }

    /**
     * Object 타입을 안전하게 int로 변환하는 유틸리티 메서드
     */
    private int convertToInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (NumberFormatException e) {
            log.warn("점수 파싱 실패: {}", obj);
            return 0;
        }
    }
}
