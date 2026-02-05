package com.example.backend.domain.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대화 분석 결과를 담는 DTO
 * AI 서버로부터 받은 대화 분석 결과를 저장
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAnalysisResult {
    
    private String taskId;
    
    @Builder.Default
    private String status = "PROCESSING"; // PROCESSING, SUCCESS, ERROR
    
    // AI 분석 결과 (구조화된 타입)
    private ConversationAnalysisDetail analysisResult;
    
    // 에러 발생 시 상태 변경
    public void markAsError() {
        this.status = "ERROR";
    }
    
    // 분석 완료 여부 확인
    public boolean isComplete() {
        return analysisResult != null;
    }
}
