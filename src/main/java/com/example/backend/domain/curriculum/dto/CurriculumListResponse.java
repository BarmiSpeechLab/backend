package com.example.backend.domain.curriculum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurriculumListResponse {
    private Long id;
    private String theme;
    private String text;
    private boolean isCompleted; // 학습 완료 여부 (시도 횟수 > 0)
    private Integer score;       // 최고 점수 (없으면 0)
    private Integer tryCount;    // 시도 횟수
}
