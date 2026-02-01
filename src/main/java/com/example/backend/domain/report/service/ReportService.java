package com.example.backend.domain.report.service;

import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.report.dto.CalendarLogResponse;
import com.example.backend.domain.report.dto.MyReportResponse;
import com.example.backend.domain.report.entity.DailyStudyLog;
import com.example.backend.domain.report.repository.DailyStudyLogRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final CurriculumStatsRepository curriculumStatsRepository;
    private final UserRepository userRepository;

    /**
     * 1. 내 학습 전체 통계 조회
     * - 총 학습일, 총 발음 횟수, 평균 정확도
     */
    public MyReportResponse getMyReport(Long userId) {
        User user = getUser(userId);

        // 1. 총 학습일 (Log가 존재하는 날짜 수)
        long totalStudyDays = dailyStudyLogRepository.countByUser(user);

        // 2. 총 발음 연습 횟수 (Sum)
        int totalTryCount = dailyStudyLogRepository.sumFeedbackCountByUser(user);

        // 3. 평균 정확도 (Avg Score)
        double rawAvg = curriculumStatsRepository.getAverageScoreByUser(user);
        double averageScore = Math.round(rawAvg * 10.0) / 10.0;

        return MyReportResponse.builder()
                .totalStudyDays(totalStudyDays)
                .totalTryCount(totalTryCount)
                .averageScore(averageScore)
                .build();
    }

    /**
     * 2. 월별 학습 캘린더 조회
     * - 특정 년/월의 날짜별 학습 횟수 반환
     */
    public List<CalendarLogResponse> getCalendarLogs(Long userId, int year, int month) {
        User user = getUser(userId);

        // 해당 월의 시작일(1일)과 마지막 날(28/29/30/31일) 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // DB 조회 (Start ~ End)
        List<DailyStudyLog> logs = dailyStudyLogRepository.findByUserAndDateBetween(user, startDate, endDate);

        // DTO 변환
        return logs.stream()
                .map(CalendarLogResponse::from)
                .collect(Collectors.toList());
    }

    // 유저 조회 헬퍼 메서드
    // TODO : 나중에 분리해야 할 것 같음
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
