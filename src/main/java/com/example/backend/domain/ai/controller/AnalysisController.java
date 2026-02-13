package com.example.backend.domain.ai.controller;

import com.example.backend.domain.ai.dto.IntegratedAnalysisResult;
import com.example.backend.domain.ai.service.AnalysisService;
import com.example.backend.global.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Tag(name = "AI 분석 API", description = "RabbitMQ 테스트용 API")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "음성 파일 제출 및 분석 요청", description = "음성 파일을 업로드하고 AI 분석을 요청합니다.")
    @PostMapping(
            value = "/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> requestAnalysis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file,
            @RequestParam Long curriculumId
    ) {
        String taskId = analysisService.requestAnalysis(file, curriculumId);
        return ResponseEntity.ok("저장 성공: " + taskId);
    }

    @Operation(summary = "음성데이터 피드백 결과 요청(Polling)", description = "음성 파일에 대한 AI 분석 결과를 요청합니다")
    @GetMapping("/{curriculumId}/{taskId}")
    public ResponseEntity<IntegratedAnalysisResult> requestResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String taskId,
            @PathVariable Long curriculumId
    ) {
        // 캐시 조회 및 DB 업데이트 (완료 시)
        IntegratedAnalysisResult result = analysisService.getResult(userDetails.getUserId(), taskId, curriculumId);
        
        // 결과 반환 (JSON 직렬화)
        return ResponseEntity.ok(result);
    }

}