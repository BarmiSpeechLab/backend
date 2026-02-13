package com.example.backend.domain.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class AnalysisResultDto {
    private Long feedbackId;      // 어떤 요청에 대한 결과인지 식별
    private Double score;         // 발음 점수
    private String feedbackText;  // AI가 해주는 조언 (텍스트)
}
