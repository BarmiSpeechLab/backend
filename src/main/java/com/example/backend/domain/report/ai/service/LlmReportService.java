package com.example.backend.domain.report.ai.service;

import com.example.backend.domain.report.ai.client.OpenAiClient;
import com.example.backend.domain.report.dto.IpaAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmReportService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    // [1] 기존 API용: AI가 준 문자열 그대로 반환 (프론트 수정 방지)
    public String generateIpaReportRaw(String prompt) {
        return openAiClient.generate(prompt);
    }

    // [2] 신규 튜터 API용: JSON 파싱 후 DTO로 반환
    public IpaAnalysisResponse generateIpaReportDto(String prompt) {
        String rawResponse = openAiClient.generate(prompt);
        try {
            String jsonOnly = rawResponse.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(jsonOnly, IpaAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            log.error("AI 응답 DTO 파싱 실패. 원본: {}", rawResponse);
            throw new RuntimeException("AI 분석 데이터 처리 오류");
        }
    }
}