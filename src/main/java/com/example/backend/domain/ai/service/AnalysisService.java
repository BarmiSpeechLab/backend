package com.example.backend.domain.ai.service;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import com.example.backend.domain.ai.dto.IntegratedAnalysisResult;
import com.example.backend.domain.ai.producer.AiClient;
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

    // 분석 요청 서비스 메서드
    // request -> AI
    public String askAnalysis(MultipartFile file) {
        // 파일저장
        String savedFileName = fileService.saveFile(file);

        // 데이터 조회
        // TODO : 정답데이터, 유저 데이터 등등 DTO로 조립
        // DB 조회 대신 하드코딩된 정답 데이터(Map) 생성
        Map<String, Object> guideData = new HashMap<>();
        guideData.put("fullText", "I like to dance");
        List<Map<String, Object>> segments = new ArrayList<>();
        Map<String, Object> word1 = new HashMap<>();
        word1.put("word", "I");
        word1.put("cipa", List.of("a", "ɪ"));
        word1.put("ckor", "아이");
        word1.put("cpl", List.of("VOWEL", "VOWEL"));
        segments.add(word1);
        Map<String, Object> word2 = new HashMap<>();
        word2.put("word", "like");
        word2.put("cipa", List.of("l", "a", "ɪ", "k"));
        word2.put("ckor", "라이크");
        word2.put("cpl", List.of("CONSONANT", "VOWEL", "VOWEL", "CONSONANT"));
        segments.add(word2);
        guideData.put("segments", segments);
        AnalysisRequestDto request = AnalysisRequestDto.builder()
                .metadata(AnalysisRequestDto.Metadata.builder()
                        .requestId("TEST_REQ_" + UUID.randomUUID().toString().substring(0, 8))
                        .userId(1L)       // ★ 테스트용 유저 ID
                        .curriculumId(101L) // ★ 테스트용 커리큘럼 ID
                        .timestamp(LocalDateTime.now().toString())
                        .build())
                .userInput(AnalysisRequestDto.UserInput.builder()
                        .fileName(savedFileName)
                        .audioFormat("wav")
                        .build())
                .guideData(guideData)
                .build();

        // 프로듀서 호출 (메시지 전송)
        aiClient.sendJob(request);
        return request.getMetadata().getRequestId();
    }

    // AI 결과 임시저장 서비스 메서드
    // AI -> Spring Cache
    // ★ 통합 저장 메서드 (하나로 통일!)
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

}
