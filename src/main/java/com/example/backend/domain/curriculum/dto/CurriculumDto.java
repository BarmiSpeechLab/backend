package com.example.backend.domain.curriculum.dto;

import com.example.backend.domain.curriculum.entity.Curriculum;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * ✅ 커리큘럼 정적 정보 (Stats 제외)
 * - 한 번 로드 후 프론트엔드에서 장기 캐싱
 */
@Getter
@Builder
public class CurriculumDto {
    private Long id;
    private String type;
    private String theme;
    private Map<String, Object> text;
    private String meaning;
    private String ipa;
    private String korPronunciation;
    private List<Map<String, Object>> inton;

    public static CurriculumDto from(Curriculum curriculum) {
        return CurriculumDto.builder()
                .id(curriculum.getId())
                .type(curriculum.getType())
                .theme(curriculum.getTheme())
                .text(curriculum.getText())
                .meaning(curriculum.getMeaning())
                .ipa(curriculum.getIpa())
                .korPronunciation(curriculum.getKorPronunciation())
                .inton(curriculum.getIntonData())
                .build();
    }
}
