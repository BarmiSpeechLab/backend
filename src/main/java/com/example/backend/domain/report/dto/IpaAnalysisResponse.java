package com.example.backend.domain.report.dto;

import java.util.List;

public record IpaAnalysisResponse(
        String summary,                // 전체 요약
        List<String> strengths,        // 강점 리스트
        List<String> weaknesses,       // 약점 리스트
        List<String> teachingStrategies, // 튜터에게 주는 가이드
        String overallLevel            // 종합 레벨
) {}
