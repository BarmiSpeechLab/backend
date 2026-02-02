package com.example.backend.domain.curriculum.entity;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import com.example.backend.domain.curriculum.model.AnalysisData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "curriculum")
public class Curriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curriculum_id")
    private Long id;

    @Column(nullable = false)
    private String type;    // 학습 종류 (예: ipa, 단어, 문장)

    @Column(nullable = false)
    private String theme;   // 학습 테마 (예: ipa, 여행, 카페...)

    @Column(nullable = false, unique = true)
    private String text;    // 영어 본문 (예: "Apple", "I like you")

    private String meaning; // 한글 뜻 (예: "사과", "나는 너를 좋아해")

    // 화면에 단순 보여주기용 (발음기호, 한글발음)
    private String ipa;
    private String korPronunciation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> cData;   // 모델이 분석한 정답데이터 저장 컬럼
}
