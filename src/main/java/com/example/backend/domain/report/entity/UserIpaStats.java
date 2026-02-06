package com.example.backend.domain.report.entity;

import com.example.backend.domain.curriculum.entity.Ipa;
import com.example.backend.domain.user.entity.User;
import com.example.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserIpaStats extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipa_id")
    private Ipa ipa;

    private Integer totalTryCount;
    private Integer successCount;

    // ✅ 시도 횟수 증가
    public void incrementTry() {
        if (this.totalTryCount == null) {
            this.totalTryCount = 1;
        } else {
            this.totalTryCount++;
        }
    }

    // ✅ 성공 횟수 증가 (시도도 함께 증가)
    public void incrementSuccess() {
        incrementTry();  // 시도도 함께 증가
        if (this.successCount == null) {
            this.successCount = 1;
        } else {
            this.successCount++;
        }
    }

    // ✅ 실패 기록 (시도만 증가)
    public void recordFailure() {
        incrementTry();
    }
}

