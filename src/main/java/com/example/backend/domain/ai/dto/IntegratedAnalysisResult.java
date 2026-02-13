package com.example.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedAnalysisResult {

    private String taskId;
    
    @Builder.Default
    private String status = "PROCESSING";
    
    private String error;  // 에러 메시지

    // 결과 데이터들
    private Object pronunciation;
    private Object intonations;
    private Object llmFeedback;

    // 에러 발생 시 상태 변경을 위한 메서드
    public void markAsError() {
        this.status = "ERROR";
    }

    // 분석 완료 여부 확인
    public boolean isComplete() {
        return pronunciation != null && intonations != null && llmFeedback != null;
    }
}
