package com.example.backend.domain.report.repository;

import com.example.backend.domain.report.entity.DailyStudyLog;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStudyLogRepository extends JpaRepository<DailyStudyLog, Long> {

    // 1. 특정 기간(Start ~ End)의 내 학습 횟수 -> 캘린더
    List<DailyStudyLog> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // 2. 나의 총 학습 일수 (로그가 존재하는 날짜 수 = 학습일수
    long countByUser(User user);

    // 3. 나의 특정 기간의 학습일수 (Start ~ End)
    long countByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // 4. 나의 총 발음 연습 횟수 (feedbackCount의 합계)
    // 기록이 없다면 0 반환 (coalesce)
    @Query("SELECT COALESCE(SUM(d.feedbackCount), 0) FROM DailyStudyLog d WHERE d.user = :user")
    Integer sumFeedbackCountByUser(@Param("user") User user);

    // 5. 오늘 날짜 기록 찾기 (나중에 학습할 때 카운트 증가시키려고 필요함)
    Optional<DailyStudyLog> findByUserAndDate(User user, LocalDate date);
}
