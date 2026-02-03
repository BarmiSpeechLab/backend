package com.example.backend.domain.curriculum.dto;

import com.example.backend.domain.curriculum.entity.Curriculum;
import com.example.backend.domain.curriculum.entity.CurriculumStats;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CurriculumResponse {
    private Long id;
    private String type;
    private String theme;
    private Map<String, Object> text;
    private String meaning;
    private String ipa;
    private String korPronunciation;
    private Map<String, Object> inton;
    private boolean isCompleted;
    private Integer score;
    private Integer tryCount;

    public static CurriculumResponse of(Curriculum curriculum, CurriculumStats stats) {
        boolean hasStats = (stats != null);

        return CurriculumResponse.builder()
                .id(curriculum.getId())
                .type(curriculum.getType())
                .theme(curriculum.getTheme())
                .text(curriculum.getText())
                .meaning(curriculum.getMeaning())
                .ipa(curriculum.getIpa())
                .korPronunciation(curriculum.getKorPronunciation())
                .inton(curriculum.getIntonData())
                .isCompleted(hasStats && stats.getTryCount() > 0)
                .score(hasStats ? stats.getScore() : 0)
                .tryCount(hasStats ? stats.getTryCount() : 0)
                .build();
    }
}
