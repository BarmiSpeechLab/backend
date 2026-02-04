package com.example.backend.domain.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Meeting 엔티티의 Response DTO
 * LAZY 로딩 문제를 피하기 위해 필요한 필드만 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    
    private Long id;
    private LocalDateTime datetime;
    private boolean isClosed;
    
    // 튜터 정보 (간소화)
    private Long tutorId;
    private String tutorNickname;
    
    // 튜티 정보 (예약 가능한 슬롯은 null)
    private Long tuteeId;
    private String tuteeNickname;
    
    private String roomId;
}
