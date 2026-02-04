package com.example.backend.domain.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대화 분석 결과 상세 정보
 * AI Gateway -> Spring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAnalysisDetail {
    
    @JsonProperty("transScript")
    private String transScript;  // 사용자 발화 음성 데이터의 텍스트 변환 결과
    
    @JsonProperty("nextTurn")
    private String nextTurn;     // prevTurn 맥락에 따른 다음 대화 내용 생성
    
    @JsonProperty("theme")
    private String theme;        // 대화 theme 유지 (DAILY, TRAVEL 등)
    
    @JsonProperty("feedback")
    private String feedback;     // AI 피드백 내용
}
