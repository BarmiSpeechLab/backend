package com.example.backend.domain.ai.dto;

import lombok.Data;
import java.util.Map;

@Data
public class IntegratedAnalysisResult {
    private String taskId;
    // 각 결과가 들어갈 자리 (아직 안 온 건 null)
    private Map<String, Object> pronunciation;
    private Map<String, Object> intonations;
    private Map<String, Object> llmFeedback;
}
