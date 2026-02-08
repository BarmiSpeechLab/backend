package com.example.backend.domain.report.service;

import com.example.backend.domain.curriculum.repository.CurriculumStatsRepository;
import com.example.backend.domain.report.dto.CalendarLogResponse;
import com.example.backend.domain.report.dto.IpaStatDto;
import com.example.backend.domain.report.dto.MyReportResponse;
import com.example.backend.domain.report.entity.DailyStudyLog;
import com.example.backend.domain.report.entity.UserIpaStats;
import com.example.backend.domain.report.repository.DailyStudyLogRepository;
import com.example.backend.domain.report.repository.UserIpaStatsRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final CurriculumStatsRepository curriculumStatsRepository;
    private final UserRepository userRepository;
    private final UserIpaStatsRepository userIpaStatsRepository;

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

    /*
    * 3. 타입 별 ipa의 정답률 데이터
    * - 계층형 데이터로 전처리해서 전송
    * */
    public Map<String, Map<String, IpaStatDto>> getIpaAnalysis(Long userId) {

        // 1. DB에서 데이터 조회 (전체 조회로 변경하여 레이더 차트 모든 카테고리 채움)
        List<UserIpaStats> statsList = userIpaStatsRepository.findAllByUserId(userId);

        // 2. 이중 Map 구조로 변환 (Stream API)
        // 중복된 IPA symbol이 있을 경우 합산
        return statsList.stream()
                .collect(Collectors.groupingBy(
                        // 1차 그룹핑: IPA 타입 (VOWEL, CONSONANT)
                        stat -> stat.getIpa().getType(),

                        // 2차 그룹핑: Key=발음기호, Value=통계DTO (중복 시 합산)
                        Collectors.toMap(
                                stat -> stat.getIpa().getSymbol(), // Key
                                stat -> IpaStatDto.builder()       // Value
                                        .totalTryCount(stat.getTotalTryCount())
                                        .successCount(stat.getSuccessCount())
                                        .accuracy(calculateAccuracy(stat.getTotalTryCount(), stat.getSuccessCount()))
                                        .build(),
                                // 중복 키가 있을 경우 합산
                                (existing, replacement) -> IpaStatDto.builder()
                                        .totalTryCount(existing.getTotalTryCount() + replacement.getTotalTryCount())
                                        .successCount(existing.getSuccessCount() + replacement.getSuccessCount())
                                        .accuracy(calculateAccuracy(
                                                existing.getTotalTryCount() + replacement.getTotalTryCount(),
                                                existing.getSuccessCount() + replacement.getSuccessCount()
                                        ))
                                        .build()
                        )
                ));
    }

    // [추가 필요] 정확도 계산 헬퍼 메서드
    private Double calculateAccuracy(Integer total, Integer success) {
        if (total == null || total == 0) return 0.0;

        double accuracy = (double) success / total * 100.0;
        // 소수점 첫째 자리까지 반올림 (예: 88.5)
        return Math.round(accuracy * 10.0) / 10.0;
    }

    // 유저 조회 헬퍼 메서드
    // TODO : 나중에 분리해야 할 것 같음
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
