package com.example.backend.domain.ai.service;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import com.example.backend.domain.ai.dto.IntegratedAnalysisResult;
import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.repository.CurriculumRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import com.example.backend.global.infra.file.FileService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public IntegratedAnalysisResult getResult(String taskId) {
        var cache = cacheManager.getCache("analysis_results");
        return (cache != null) ? cache.get(taskId, IntegratedAnalysisResult.class) : null;
    }

}
