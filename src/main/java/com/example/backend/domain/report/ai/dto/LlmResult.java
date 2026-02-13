package com.example.backend.domain.report.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LlmResult {

    /** 실제 AI가 생성한 리포트 텍스트 */
    private String content;

    /** 사용한 LLM 종류 */
    private String provider;

    /** 토큰 사용량 (선택, 나중에 확장용) */
    private Integer usedTokens;
}
