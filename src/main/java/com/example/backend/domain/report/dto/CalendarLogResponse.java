package com.example.backend.domain.report.dto;

import com.example.backend.domain.report.entity.DailyStudyLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CalendarLogResponse {
    private LocalDate date;
    private int count; // 해당일의 학습 횟수

    public static CalendarLogResponse from(DailyStudyLog log) {
        return CalendarLogResponse.builder()
                .date(log.getDate())
                .count(log.getFeedbackCount())
                .build();
    }
}
