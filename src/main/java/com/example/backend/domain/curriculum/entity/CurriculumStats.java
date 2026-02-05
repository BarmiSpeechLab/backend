package com.example.backend.domain.curriculum.entity;

import com.example.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumStats {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id")
    private Curriculum curriculum;

    private Integer tryCount; // 시도 횟수

    @Column(nullable = false)
    private Integer score;

    // AI 분석 결과 추가 정보
    private Double errorRate;    // 오답률 (0.0 ~ 1.0)
    private Integer errorLevel;  // 오답 레벨 (0-3)

    public void updateScore(int score) {
        if(this.score < score) this.score = score;
    }

    // 시도 횟수 증가
    public void increaseTryCount() {
        if (this.tryCount == null) {
            this.tryCount = 1;
        } else {
            this.tryCount++;
        }
    }

    // ✅ error_rate와 error_level 업데이트 (최신 값으로 덮어쓰기)
    public void updateErrorMetrics(Double errorRate, Integer errorLevel) {
        this.errorRate = errorRate;
        this.errorLevel = errorLevel;
    }

    /**
     * ✅ grade 계산 (error_level 기반)
     * @return Grade (1=Perfect ~ 4=Try Again)
     */
    public Grade getGrade() {
        return Grade.fromErrorLevel(this.errorLevel);
    }

    /**
     * ✅ grade level 반환 (숫자)
     * @return 1~4
     */
    public int getGradeLevel() {
        return getGrade().getLevel();
    }
}
