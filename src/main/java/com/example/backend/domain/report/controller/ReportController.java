package com.example.backend.domain.report.controller;

import com.example.backend.domain.report.dto.CalendarLogResponse;
import com.example.backend.domain.report.dto.IpaAnalysisResponse;
import com.example.backend.domain.report.dto.IpaStatDto;
import com.example.backend.domain.report.dto.MyReportResponse;
import com.example.backend.domain.report.service.ReportService;
import com.example.backend.global.dto.ApiResponse;
import com.example.backend.global.userdetails.CustomUserDetails;
import com.example.backend.domain.report.ai.prompt.IpaPromptBuilder;
import com.example.backend.domain.report.ai.service.LlmReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "유저 학습 리포트 API", description = "유저 발음학습 통계 및 로그를 제공하는 API 입니다.")
public class ReportController {

    private final ReportService reportService;
    private final LlmReportService llmReportService;

    @Operation(summary = "나의 전체 통계 조회 API",description = "특정 유저의 전체 발음학습 통계 데이터를 응답합니다.")
    @GetMapping("/my-stats")
    public ResponseEntity<ApiResponse<MyReportResponse>> getMyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getMyReport(userDetails.getUserId())
        ));
    }

    @Operation(summary = "학습 캘린더 조회 API", description = "캘린더형 학습 기록을 위한 로그 데이터를 응답합니다.")
    @GetMapping("/calendar/{year}/{month}")
    public ResponseEntity<ApiResponse<List<CalendarLogResponse>>> getCalendarLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable int year,
            @PathVariable int month) {

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getCalendarLogs(userDetails.getUserId(), year, month)
        ));
    }

    @Operation(summary = "타입별 IPA 정답률 API", description = "레이더 차트 랜더링을 위한 타입별 IPA 정답률 데이터를 응답합니다.")
    @GetMapping("/radar-chart")
    public ResponseEntity<ApiResponse<Map<String, Map<String, IpaStatDto>>>> getIpaStats(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getIpaAnalysis(userDetails.getUserId())
        ));
    }

    @Operation(summary = "튜터 전용 타겟 학생 IPA 정답률 조회 API", description = "튜터가 학생의 발음 정답률 데이터를 조회합니다.")
    @GetMapping("/radar-chart/{tuteeId}")
    @PreAuthorize("hasRole('TUTOR') or #userDetails.userId == #tuteeId")
    public ResponseEntity<ApiResponse<Map<String, Map<String, IpaStatDto>>>> getStudentIpaStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tuteeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getIpaAnalysis(tuteeId)
        ));
    }

    @Operation(summary = "나의 AI 학습 분석 리포트", description = "자신의 IPA 발음 통계를 기반으로 AI 분석 리포트를 제공합니다.")
    @GetMapping("/radar-chart/ai-report")
    public ResponseEntity<ApiResponse<String>> getMyIpaAiReport(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var stats = reportService.getIpaAnalysis(userDetails.getUserId());
        String prompt = IpaPromptBuilder.build(stats);
        String aiReport = llmReportService.generateIpaReportRaw(prompt);

        return ResponseEntity.ok(ApiResponse.success(aiReport));
    }

    @Operation(summary = "튜터 전용 학생 AI 학습 분석 리포트")
    @GetMapping("/radar-chart/ai-report/{tuteeId}")
    @PreAuthorize("hasRole('TUTOR') or #userDetails.userId == #tuteeId")
    public ResponseEntity<ApiResponse<IpaAnalysisResponse>> getStudentIpaAiReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tuteeId
    ) {
        var stats = reportService.getIpaAnalysis(tuteeId);
        String prompt = IpaPromptBuilder.build(stats);

        IpaAnalysisResponse aiReport = llmReportService.generateIpaReportDto(prompt);

        return ResponseEntity.ok(ApiResponse.success(aiReport));
    }
}