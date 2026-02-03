package com.example.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PronResult {
    @JsonProperty("target_word")
    private String targetWord; // 정답 단어 (예: APPLE)

    private String word;       // 사용자가 발음한 단어

    private List<Phoneme> phonemes; // 음소 단위 상세 분석 리스트

    private KoreanComparison kor;   // 한글 발음 비교 (애플 vs 애벌)

    @JsonProperty("error_rate")
    private double errorRate;  // 에러율

    @JsonProperty("error_level")
    private int errorLevel;    // 에러 레벨 (1~3 등)

    @JsonProperty("is_correct")
    private boolean isCorrect; // 최종 정답 여부

    // 비즈니스 로직에서 점수가 필요할 경우를 위한 편의 메서드
    public int getScore() {
        return (int) ((1.0 - errorRate) * 100);
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Phoneme {
    private String cpl;  // Correct Phoneme (정답 음소)
    private String upl;  // User Phoneme (사용자 음소)
    private String cipa; // Correct IPA
    private String uipa; // User IPA
    private String type; // vowel(모음), consonant(자음) 등

    @JsonProperty("is_correct")
    private boolean isCorrect;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class KoreanComparison {
    private String ckor; // Correct Korean (정답 한글 표기)
    private String ukor; // User Korean (사용자 발음 한글 표기)
}