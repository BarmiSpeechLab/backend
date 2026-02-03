package com.example.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedAnalysisResult {

    private String taskId;
    private String status = "PENDING";
    private String errorMessage;

    // 결과 데이터들
    private Map<String, Object> pronunciation;
    private Map<String, Object> intonations;
    private Map<String, Object> llmFeedback;

    // 에러 마킹용
    public void markAsError(String message) {
        this.status = "ERROR";
        this.errorMessage = message;
    }

    // 분석 완료 여부 확인
    public boolean isComplete() {
        return pronunciation != null && intonations != null && llmFeedback != null;
    }
}
