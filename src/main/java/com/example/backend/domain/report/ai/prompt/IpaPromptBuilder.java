package com.example.backend.domain.report.ai.prompt;

import com.example.backend.domain.report.dto.IpaStatDto;

import java.util.Map;

public class IpaPromptBuilder {

    public static String build(Map<String, Map<String, IpaStatDto>> stats) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
        당신은 영어 발음 학습을 분석하는 전문 튜터입니다.
        아래는 사용자의 IPA 발음 정답률 통계입니다.
        이를 기반으로 학습 리포트를 작성해주세요.

        [출력 형식 가이드]
        - 반드시 Markdown 문법을 사용하세요.
        - '## 강점', '## 약점', '## 꿀팁' 처럼 소제목을 달아주세요.
        - 중요한 단어는 **굵게** 표시해서 가독성을 높여주세요.
        - 말투는 똑똑한 비서처럼 해주세요.

        요구사항:
        1. 잘하는 영역
        2. 부족한 영역
        3. 개선을 위한 구체적인 연습 방향
        4. 전체 요약

        통계 데이터:
        """);

        stats.forEach((type, ipaMap) -> {
            sb.append("\n[").append(type).append("]\n");
            ipaMap.forEach((ipa, dto) -> {
                sb.append("- ")
                  .append(ipa)
                  .append(" : ")
                  .append(dto.getAccuracy())
                  .append("%\n");
            });
        });

        return sb.toString();
    }
}
