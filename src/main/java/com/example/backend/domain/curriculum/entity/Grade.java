package com.example.backend.domain.curriculum.entity;

import lombok.Getter;

/**
 * 발음 평가 등급 (error_level 기반)
 * - 낮을수록 좋음 (1 = Perfect, 4 = Try Again)
 */
@Getter
public enum Grade {
    PERFECT(1, "Perfect", "완벽해요! 🎉"),
    GOOD(2, "Good", "잘했어요! 👍"),
    FAIR(3, "Fair", "조금만 더! 💪"),
    TRY_AGAIN(4, "Try Again", "다시 도전! 🔥");

    private final int level;
    private final String label;
    private final String message;

    Grade(int level, String label, String message) {
        this.level = level;
        this.label = label;
        this.message = message;
    }

    /**
     * error_level → Grade 변환
     * @param errorLevel AI 응답의 error_level (0~3)
     * @return 해당하는 Grade
     */
    public static Grade fromErrorLevel(Integer errorLevel) {
        if (errorLevel == null) {
            return TRY_AGAIN;  // 기본값
        }
        
        // error_level 0 → Grade 1 (PERFECT)
        // error_level 1 → Grade 2 (GOOD)
        // error_level 2 → Grade 3 (FAIR)
        // error_level 3+ → Grade 4 (TRY_AGAIN)
        return switch (errorLevel) {
            case 0 -> PERFECT;
            case 1 -> GOOD;
            case 2 -> FAIR;
            default -> TRY_AGAIN;
        };
    }

    /**
     * grade level → Grade 변환
     * @param gradeLevel 등급 레벨 (1~4)
     * @return 해당하는 Grade
     */
    public static Grade fromLevel(int gradeLevel) {
        return switch (gradeLevel) {
            case 1 -> PERFECT;
            case 2 -> GOOD;
            case 3 -> FAIR;
            case 4 -> TRY_AGAIN;
            default -> TRY_AGAIN;
        };
    }
}
