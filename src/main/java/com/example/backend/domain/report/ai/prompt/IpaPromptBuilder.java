package com.example.backend.domain.report.ai.prompt;

import com.example.backend.domain.report.dto.IpaStatDto;

import java.util.Map;

public class IpaPromptBuilder {
    public static String build(Map<String, Map<String, IpaStatDto>> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
        당신은 영어 발음 데이터를 분석하여 전문적인 교육 전략을 수립하는 AI 어시스턴트입니다.
        학습자의 IPA 발음 통계를 분석하여, 담당 튜터가 지도할 때 참고할 '학습 분석 보고서'를 작성하세요.

        [보고 가이드]
        1. 튜터가 학생을 어떻게 가르치면 좋을지 구체적인 교수법을 제안하세요.
        2. 말투는 보고하는 형식(~입니다, ~하는 것을 권장합니다)으로 작성하세요.
        3. 반드시 아래 JSON 구조로만 응답하세요. (Markdown 기호 제외)

        {
          "summary": "학습 상태 전체 요약",
          "strengths": ["강점 1", "강점 2"],
          "weaknesses": ["약점 1", "약점 2"],
          "teachingStrategies": ["튜터 지침 1: ~하게 지도하세요", "튜터 지침 2: ~를 강조하세요"],
          "overallLevel": "분석된 전체 레벨"
        }

        [데이터 정보]
        """);

        stats.forEach((type, ipaMap) -> {
            sb.append("\n[").append(type).append("]\n");
            ipaMap.forEach((ipa, dto) -> {
                sb.append("- ").append(ipa).append(" : ").append(dto.getAccuracy()).append("%\n");
            });
        });

        return sb.toString();
    }
}
