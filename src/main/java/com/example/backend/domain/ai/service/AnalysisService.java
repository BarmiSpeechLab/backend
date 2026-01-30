package com.example.backend.domain.ai.service;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.global.infra.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final AiClient aiClient;
    private final FileService fileService;

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

}
