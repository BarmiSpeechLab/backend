package com.example.backend.domain.ai.controller;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import com.example.backend.domain.ai.producer.AiClient;
import com.example.backend.domain.ai.service.AnalysisService;
import com.example.backend.global.infra.file.FileService;
import com.example.backend.global.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "AI 분석 API", description = "RabbitMQ 테스트용 API")
@RestController
@RequestMapping("/api/feedback")
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
            @RequestPart("file") MultipartFile file
    ) {
        String taskId = analysisService.askAnalysis(file);
        return ResponseEntity.ok("저장 성공: " + taskId);
    }

    @Operation(summary = "음성데이터 피드백 결과 요청", description = "음성 파일에 대한 AI 분석 결과를 요청합니다")
    @GetMapping
    public ResponseEntity<String> requestResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String taskId
    ) {
        /*
          TODO
            1.taskId로 캐시에 저장된 전체 피드백 결과를 가져오기
            2.result를 결과 통계에 업데이트
            3.user log(방문기록) 테이블 업데이트
            4.전체 결과 반환
        */
        return ResponseEntity.ok("저장 성공: " + taskId);
    }

}