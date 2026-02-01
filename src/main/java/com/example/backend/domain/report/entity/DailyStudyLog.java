package com.example.backend.domain.report.entity;

import com.example.backend.domain.user.entity.User;
import com.example.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "daily_study_logs",
        uniqueConstraints = {
                // 유저당 '하루에 하나'의 기록만 존재해야 하므로 유니크 제약조건
                @UniqueConstraint(
                        name = "uk_daily_study_user_date",
                        columnNames = {"user_id", "date"}
                )
        }
)
public class DailyStudyLog extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date; // 학습 날짜

    @Column(nullable = false)
    private Integer feedbackCount; // AI 피드백 요청 횟수 (학습량)

    // 생성자 (로그인 시 호출 - 카운트는 0부터 시작)
    @Builder
    public DailyStudyLog(User user, LocalDate date) {
        this.user = user;
        this.date = date;
        this.feedbackCount = 0;   // 최초 생성시 카운트 0
    }

    // 피드백 요청할 때마다 호출
    public void increaseFeedbackCount() {
        this.feedbackCount++;
    }
}
