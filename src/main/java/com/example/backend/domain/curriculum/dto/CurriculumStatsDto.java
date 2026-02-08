package com.example.backend.domain.curriculum.dto;

import com.example.backend.domain.curriculum.entity.CurriculumStats;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자별 커리큘럼 통계 (동적 데이터)
 * - 매번 조회하여 최신 데이터 제공
 */
@Getter
@Builder
public class CurriculumStatsDto {
    private Long curriculumId;
    private Integer score;
    private Integer tryCount;
    private Double errorRate;
    private Integer errorLevel;
    private Integer grade;  // 1=Perfect ~ 4=Try Again

    public static CurriculumStatsDto from(CurriculumStats stats) {
        return CurriculumStatsDto.builder()
                .curriculumId(stats.getCurriculum().getId())
                .score(stats.getScore())
                .tryCount(stats.getTryCount())
                .errorRate(stats.getErrorRate())
                .errorLevel(stats.getErrorLevel())
                .grade(stats.getGradeLevel())
                .build();
    }
}
