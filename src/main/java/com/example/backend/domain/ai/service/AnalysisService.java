package com.example.backend.domain.ai.service;

import com.example.backend.domain.ai.dto.IntegratedAnalysisResult;
import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import com.example.backend.domain.curriculum.entity.Ipa;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.curriculum.repository.IpaRepository;
import com.example.backend.domain.curriculum.service.CurriculumService;
import com.example.backend.domain.curriculum.service.IpaCacheService;
import com.example.backend.domain.report.entity.DailyStudyLog;
import com.example.backend.domain.report.entity.UserIpaStats;
import com.example.backend.domain.report.repository.DailyStudyLogRepository;
import com.example.backend.domain.report.repository.UserIpaStatsRepository;
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
    private final CurriculumService curriculumService;
    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final UserRepository userRepository;
    private final IpaRepository ipaRepository;
    private final UserIpaStatsRepository userIpaStatsRepository;
    private final IpaCacheService ipaCacheService;  // IPA 캐시 서비스

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
        log.info("[DB 저장 시작] user={}, curriculum={}", user.getNickname(), curriculum.getId());
        
        // 1. ✅ error_rate, error_level, score 추출
        int[] metrics = extractErrorMetrics(result);
        int score = metrics[0];           // score (0-100)
        int errorRatePercent = metrics[1]; // error_rate를 퍼센트로 (0-100)
        int errorLevel = metrics[2];       // error_level (0-2)
        
        double errorRate = errorRatePercent / 100.0;  // 다시 0.0~1.0 범위로
        
        log.info("[메트릭 추출 완료] score={}, errorRate={}, errorLevel={}", 
                score, errorRatePercent, errorLevel);
        
        // 2. CurriculumStats (커리큘럼별 최고기록/완료여부) 업데이트
        CurriculumStats stats = curriculumStatsRepository.findByUserAndCurriculumId(user, curriculum.getId())
                .orElseGet(() -> CurriculumStats
                        .builder()
                        .curriculum(curriculum)
                        .user(user)
                        .score(0)
                        .tryCount(0)
                        .build());

        stats.updateScore(score);       // 점수 업데이트 (최고 점수만 저장)
        stats.increaseTryCount();        // 시도 횟수 증가
        stats.updateErrorMetrics(errorRate, errorLevel);  // ✅ error_rate, error_level 저장
        curriculumStatsRepository.save(stats);
        log.info("[CurriculumStats 저장] tryCount={}, score={}, errorRate={}, errorLevel={}", 
                stats.getTryCount(), stats.getScore(), stats.getErrorRate(), stats.getErrorLevel());

        // ✅ 3. UserIpaStats (IPA별 통계) 업데이트
        updateUserIpaStats(user, result);

        // 4. DailyStudyLog (일일 학습량) 업데이트
        DailyStudyLog todayLog = dailyStudyLogRepository.findByUserAndDate(user, LocalDate.now())
                .orElseGet(() -> DailyStudyLog.builder()
                        .user(user)
                        .date(LocalDate.now())
                        .feedbackCount(0)
                        .build());
        
        todayLog.increaseFeedbackCount();
        dailyStudyLogRepository.save(todayLog);
        
        log.info("[DB 저장 완료] User={}, Score={}, ErrorRate={}, ErrorLevel={}, TryCount={}, Curriculum={}", 
                user.getNickname(), score, errorRate, errorLevel, stats.getTryCount(), curriculum.getId());
    }

    /**
     * AI 분석 결과에서 error_rate와 error_level을 추출하는 메서드
     * @return [score, errorRate (0.43 → 43), errorLevel]
     */
    private int[] extractErrorMetrics(IntegratedAnalysisResult result) {
        if (!(result.getPronunciation() instanceof Map<?, ?> pronMap)) {
            log.warn("[Error 추출 실패] pronunciation이 Map 타입이 아님");
            return new int[]{0, 0, 0};
        }

        // 실제 AI 응답은 snake_case (analysis_result)
        Object analysisResultObj = pronMap.get("analysis_result");
        
        // analysis_result가 List인지 확인
        if (!(analysisResultObj instanceof List<?> analysisList) || analysisList.isEmpty()) {
            log.warn("[Error 추출 실패] analysis_result가 비어있거나 List 타입이 아님");
            return new int[]{0, 0, 0};
        }

        double totalErrorRate = 0.0;
        int totalErrorLevel = 0;
        int count = 0;

        // 각 단어별 error_rate와 error_level 누적
        for (Object item : analysisList) {
            if (item instanceof Map<?, ?> itemMap) {
                Object errorRateObj = itemMap.get("error_rate");
                Object errorLevelObj = itemMap.get("error_level");
                
                if (errorRateObj != null) {
                    double errorRate = 0.0;
                    if (errorRateObj instanceof Number) {
                        errorRate = ((Number) errorRateObj).doubleValue();
                    } else {
                        try {
                            errorRate = Double.parseDouble(String.valueOf(errorRateObj));
                        } catch (NumberFormatException e) {
                            log.warn("[error_rate 파싱 실패] value={}", errorRateObj);
                        }
                    }
                    
                    int errorLevel = convertToInteger(errorLevelObj);
                    
                    totalErrorRate += errorRate;
                    totalErrorLevel += errorLevel;
                    count++;
                    
                    log.debug("[Error 추출] word={}, error_rate={}, error_level={}", 
                            itemMap.get("word"), errorRate, errorLevel);
                }
            }
        }

        if (count == 0) {
            log.warn("[Error 추출 실패] 유효한 error 데이터가 없음");
            return new int[]{0, 0, 0};
        }

        double avgErrorRate = totalErrorRate / count;  // 0.0 ~ 1.0 (또는 1.0 초과 가능)
        int avgErrorLevel = totalErrorLevel / count;    // 0 ~ 3
        
        // ✅ error_rate > 1.0인 경우 음수 방지 (0점 처리)
        int avgScore = Math.max(0, (int) ((1.0 - avgErrorRate) * 100));
        
        int errorRatePercent = (int) (avgErrorRate * 100);  // 0.43 → 43
        
        log.info("[Error 메트릭 계산] avgErrorRate={}, avgErrorLevel={}, avgScore={}", 
                avgErrorRate, avgErrorLevel, avgScore);
        
        return new int[]{avgScore, errorRatePercent, avgErrorLevel};
    }

    /**
     * Object 타입을 안전하게 int로 변환하는 유틸리티 메서드
     */
    private int convertToInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("[타입 변환 실패] obj={}, error={}", obj, e.getMessage());
            return 0;
        }
    }

    /**
     * AI 분석 결과에서 phoneme 데이터를 추출하여 UserIpaStats 업데이트
     */
    private void updateUserIpaStats(User user, IntegratedAnalysisResult result) {
        if (!(result.getPronunciation() instanceof Map<?, ?> pronMap)) {
            log.warn("[IPA 통계 업데이트 건너뜀] pronunciation이 Map 타입이 아님");
            return;
        }

        // 실제 AI 응답은 snake_case (analysis_result)
        Object analysisResultObj = pronMap.get("analysis_result");
        if (!(analysisResultObj instanceof List<?> analysisList)) {
            log.warn("[IPA 통계 업데이트 건너뜀] analysis_result가 List 타입이 아님");
            return;
        }

        log.info("[IPA 통계 업데이트 시작] user={}, 단어 수={}", user.getNickname(), analysisList.size());

        int updatedCount = 0;
        int processedPhonemes = 0;

        // 각 단어별로 phoneme 데이터 처리
        for (Object item : analysisList) {
            if (!(item instanceof Map<?, ?> itemMap)) continue;

            // phonemes 배열 가져오기
            Object phonemesObj = itemMap.get("phonemes");
            if (!(phonemesObj instanceof List<?> phonemesList)) {
                log.debug("[Phoneme 없음] word={}", itemMap.get("word"));
                continue;
            }

            // 각 phoneme 처리
            for (Object phonemeObj : phonemesList) {
                if (!(phonemeObj instanceof Map<?, ?> phonemeMap)) continue;

                processedPhonemes++;

                // cipa (올바른 IPA 기호) 추출
                String cipaSymbol = String.valueOf(phonemeMap.get("cipa"));
                if (cipaSymbol == null || cipaSymbol.equals("null") || cipaSymbol.isEmpty()) {
                    log.debug("[IPA 기호 없음] phoneme 건너뜀");
                    continue;
                }

                // is_correct 추출
                Boolean isCorrect = null;
                Object isCorrectObj = phonemeMap.get("is_correct");
                if (isCorrectObj instanceof Boolean) {
                    isCorrect = (Boolean) isCorrectObj;
                } else if (isCorrectObj != null) {
                    isCorrect = Boolean.valueOf(String.valueOf(isCorrectObj));
                }

                if (isCorrect == null) {
                    log.debug("[is_correct 없음] cipa={}", cipaSymbol);
                    continue;
                }

                // ✅ IPA 엔티티 조회 (캐시 사용, DB 조회 없음)
                Ipa cachedIpa = ipaCacheService.getBySymbol(cipaSymbol);
                
                // IPA가 캐시에 없으면 (DB에도 없으면) 생성
                final Ipa ipa;
                if (cachedIpa == null) {
                    String type = String.valueOf(phonemeMap.get("type"));
                    Ipa newIpa = Ipa.builder()
                            .symbol(cipaSymbol)
                            .type(type != null && !type.equals("null") ? type : "unknown")
                            .build();
                    ipa = ipaRepository.save(newIpa);  // DB에 저장
                    ipaCacheService.addToCache(ipa);   // 캐시에 추가
                    log.info("[신규 IPA 생성 및 캐시 추가] symbol={}, type={}, id={}", 
                            cipaSymbol, type, ipa.getId());
                } else {
                    ipa = cachedIpa;
                }

                // UserIpaStats 조회 또는 생성
                UserIpaStats stats = userIpaStatsRepository.findByUserAndIpa(user, ipa)
                        .orElseGet(() -> UserIpaStats.builder()
                                .user(user)
                                .ipa(ipa)
                                .totalTryCount(0)
                                .successCount(0)
                                .build());

                // 통계 업데이트
                if (isCorrect) {
                    stats.incrementSuccess();  // 시도 + 성공 모두 증가
                    log.debug("[IPA 성공] cipa={}, ipaId={}, totalTry={}, success={}",
                            cipaSymbol, ipa.getId(), stats.getTotalTryCount(), stats.getSuccessCount());
                } else {
                    stats.recordFailure();  // 시도만 증가
                    log.debug("[IPA 실패] cipa={}, ipaId={}, totalTry={}, success={}",
                            cipaSymbol, ipa.getId(), stats.getTotalTryCount(), stats.getSuccessCount());
                }

                userIpaStatsRepository.save(stats);
                updatedCount++;
            }
        }

        log.info("[IPA 통계 업데이트 완료] processedPhonemes={}, updatedStats={}",
                processedPhonemes, updatedCount);
    }
}
