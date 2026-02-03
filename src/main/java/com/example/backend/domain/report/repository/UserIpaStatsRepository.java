package com.example.backend.domain.report.repository;

import com.example.backend.domain.report.entity.UserIpaStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserIpaStatsRepository extends JpaRepository<UserIpaStats, Long> {

    // 리포트 서비스에서 자주 틀린 발음 Top 5 뽑을 때 사용하는 메서드 (예시)
    // 필요에 따라 메서드 이름은 ReportService에서 호출하는 것과 똑같이 맞춰야 합니다.
    List<UserIpaStats> findTop5ByUser_IdOrderByTotalTryCountDesc(Long userId);
}
