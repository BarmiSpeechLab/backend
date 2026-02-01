package com.example.backend.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReportResponse {
    private long totalStudyDays;    // 총 학습 일수
    private int totalTryCount;      // 총 발음 연습 횟수
    private double averageScore;    // 평균 정확도
}
