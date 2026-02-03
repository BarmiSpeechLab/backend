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

        // 3. 만약 AI 서버가 '문자열'이 아닌 '객체' 형태를 원한다면
        // 아래와 같이 요청 데이터를 조립합니다.
        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("filePath", savedFileName);
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
        if (taskId == null) {
            log.error("TaskId가 누락된 결과가 수신되었습니다. Type: {}", type);
            return;
        }
        // 1. 스프링 캐시 매니저에서 껍데기(Spring Cache) 가져오기
        org.springframework.cache.Cache springCache = cacheManager.getCache("analysis_results");
        if (springCache == null) return;    // 동적생성금지 옵션이 켜지거나 다른 캐시매니저로 교체했을때를 대비

        // 2. Caffeine Cache로 형 변환
        // getNativeCache()는 Object를 리턴하므 (Cache)로 캐스팅해야 asMap() 사용가능
        Cache<Object, Object> caffeineCache = (Cache<Object, Object>) springCache.getNativeCache();


        // 2. 동시성 문제 해결을 위해 asMap().compute() 사용
        // (RabbitMQ 리스너들이 동시에 도착해도 데이터가 안 씹히게 함)
        caffeineCache.asMap().compute(taskId, (key, existingValue) -> {

            // 기존 값이 있으면 쓰고, 없으면(null) 새로 만듦
            IntegratedAnalysisResult report = (IntegratedAnalysisResult) existingValue;
            if (report == null) {
                report = new IntegratedAnalysisResult();
                report.setTaskId(taskId);
            }

            // 3. 타입에 따라 "해당 칸"에만 데이터 채우기 (Switch 문)
            switch (type) {
                case "PRON" -> report.setPronunciation(rawData);
                case "INTON" -> report.setIntonations(rawData);
                case "LLM" -> report.setLlmFeedback(rawData);
            }

            return report; // 업데이트된 객체 리턴 (캐시에 자동 저장됨)
        });

        log.info("데이터 병합 완료 [Type: {}] TaskId: {}", type, taskId);
    }
    // 조회 메서드
    @Transactional
    public IntegratedAnalysisResult getResult(Long userId, String taskId, Long curriculumId) {
        // 1. 캐시 가져오기
        org.springframework.cache.Cache cache = cacheManager.getCache("analysis_results");
        if (cache == null) return null;

        // 2. TaskId로 데이터 조회
        // (CacheWrapper에서 실제 값 꺼내기)
        IntegratedAnalysisResult result = cache.get(taskId, IntegratedAnalysisResult.class);
        if (result == null) return null;            // result가 null일때 바로 null 반환
        if ("ERROR".equals(result.getStatus())) {   // status가 error일 때 바로 error 반환
            cache.evict(taskId); // 캐시 삭제
            return result;
        }
        // 분석 완료 (덮어쓰기해서 하나씩 )
        // DB 로직은 세 개 다 있어야 실행된다
        if (isAnalysisComplete(result)){
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            Curriculum curriculum = curriculumRepository.findById(curriculumId).orElseThrow(() -> new CustomException(ErrorCode.EXPRESSION_NOT_FOUND));
            // DB 업데이트
            saveToDatabase(user, result, curriculum);
            cache.evict(taskId);    // DB 저장 후 캐시 삭제
            result.setStatus("SUCCESS"); // 클라이언트에게 최종 완료 알림
        }
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
