package com.example.backend.domain.ai.service;

import com.example.backend.domain.ai.dto.IntegratedAnalysisResult;
import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.report.repository.DailyStudyLogRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import com.example.backend.global.infra.file.FileService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        // 요청 데이터 조립 (Map 사용)
        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);             // 식별자
        request.put("filePath", savedFileName);    // 파일 경로
        request.put("type", curriculum.getType());
        request.put("analysisRequest", curriculum.getCData());  // 정답 데이터

        // 프로듀서 호출 (메시지 전송)
        aiClient.sendJob(request);

        return taskId;
    }

    // AI 결과 통합 저장 서비스 메서드
    // AI -> Spring Cache
    public void saveResult(String taskId, String type, Map<String, Object> rawData) {
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
    public IntegratedAnalysisResult getResult(Long userId, String taskId, Long curriculumId) {
        // 1. 캐시 가져오기
        org.springframework.cache.Cache cache = cacheManager.getCache("analysis_results");
        if (cache == null) return null;

        // 2. TaskId로 데이터 조회
        // (CacheWrapper에서 실제 값 꺼내기)
        IntegratedAnalysisResult result = cache.get(taskId, IntegratedAnalysisResult.class);

        // 데이터가 없으면 null 반환
        if (result == null) {
            return null;
        }

        // 분석 완료 (덮어쓰기해서 하나씩 )
        // DB 로직은 세 개 다 있어야 실행된다
        if (isAnalysisComplete(result)){
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    //        Curriculum curriculum = curricndById(userId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
            Curriculum curriculum = Curriculum.builder().id(1L).build();

            // 3. DB 업데이트 (점수, 로그 등)
            saveToDatabase(user, result, curriculum);

            // 4. 캐시 삭제 (이미 DB에 저장했으므로 메모리 확보 + 중복 저장 방지)
            cache.evict(taskId);

        }
        return result;
    }
    private boolean isAnalysisComplete(IntegratedAnalysisResult result) {
        return result.getPronunciation() != null
                && result.getIntonations() != null   // ★ 여기랑
                && result.getLlmFeedback() != null;  // ★ 여기가 null이면 무조건 false
    }
    // DB 저장 로직 분리
    private void saveToDatabase(User user, IntegratedAnalysisResult result, Curriculum curriculum) {
        // 1. 점수 추출 (예: 발음 점수가 메인 점수라고 가정)
        int score = (int) result.getPronunciation().getOrDefault("score", 0);
        // TODO : String으로 넘어오는 경우 파싱 필요: Integer.parseInt(String.valueOf(...))\

        // 2. CurriculumStats (커리큘럼별 최고기록/완료여부) 업데이트
//        CurriculumStats stats = curriculumStatsRepository.findByUserAndCurriculumId(user, curriculum.getId())
//                .orElseGet(() -> CurriculumStats
//                        .builder()
//                        .curriculum(curriculum)
//                        .user(user)
//                        .build());
//        stats.updateScore(score);
//        curriculumStatsRepository.save(stats);

        // 3. DailyStudyLog (일일 학습량) 업데이트
//        DailyStudyLog todayLog = dailyStudyLogRepository.findByUserAndDate(user, LocalDate.now())
//                .orElseGet(() -> new DailyStudyLog(user, LocalDate.now()));
//        todayLog.increaseFeedbackCount();
//        dailyStudyLogRepository.save(todayLog);
//
//        log.info("=======stats======");
//        log.info(stats.toString());
//        log.info("=======log======");
//        log.info(todayLog.toString());
        log.info("DB save 메서드 실행");
    }

}
