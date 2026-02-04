package com.example.backend.domain.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대화 분석 요청 데이터
 * Spring -> AI Gateway
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRequest {
    
    private String prevTurn;  // 이전 대화 내용
    private String theme;     // 대화 주제 (DAILY, TRAVEL 등)
}
