package com.example.backend.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IpaStatDto {
    private Integer totalTryCount;  // 시도 횟수
    private Integer successCount;   // 성공 횟수
    private Double accuracy;        // 정답률
}
