package com.example.backend.domain.curriculum.dto;

import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurriculumListResponse {
    private Long id;
    private String type;             // 학습 종류 (ipa, 단어...)
    private String theme;            // 테마
    private String text;             // 영어 본문
    private String meaning;          // 한글 뜻
    private String ipa;              // 발음기호

    // 합쳐진 진행도 데이터
    private boolean isCompleted;     // 학습 완료 여부
    private Integer score;           // 최고 점수 (없으면 0)
    private Integer tryCount;        // 시도 횟수 (없으면 0)

    // 생성 메서드 (커리큘럼 + 통계 -> DTO 변환)
    public static CurriculumListResponse of(Curriculum curriculum, CurriculumStats stats) {
        boolean hasStats = (stats != null);

        return CurriculumListResponse.builder()
                .id(curriculum.getId())
                .type(curriculum.getType())
                .theme(curriculum.getTheme())
                .text(curriculum.getText())
                .meaning(curriculum.getMeaning())
                .ipa(curriculum.getIpa())
                // 기록이 있으면 넣고, 없으면 기본값(false, 0)
                .isCompleted(hasStats && stats.getTryCount() > 0)
                .score(hasStats ? stats.getScore() : 0)
                .tryCount(hasStats ? stats.getTryCount() : 0)
                .build();
    }
}
