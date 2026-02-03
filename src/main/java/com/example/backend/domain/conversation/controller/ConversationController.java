package com.example.backend.domain.conversation.controller;

import com.example.backend.domain.conversation.dto.ConversationAnalysisResult;
import com.example.backend.domain.conversation.service.ConversationService;
import com.example.backend.global.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Conversation 분석 API", description = "대화 분석 관련 API")
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "대화 파일 제출 및 분석 요청", description = "대화 음성 파일을 업로드하고 AI 분석을 요청합니다.")
    @PostMapping(
            value = "/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> requestAnalysis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file,
            @RequestParam Long curriculumId
    ) {
        String taskId = conversationService.requestConversationAnalysis(file, curriculumId);
        return ResponseEntity.ok("대화 분석 요청 성공: " + taskId);
    }

    @Operation(summary = "대화 분석 결과 요청(Polling)", description = "대화 분석 결과를 폴링으로 조회합니다")
    @GetMapping("/{taskId}")
    public ResponseEntity<ConversationAnalysisResult> getResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String taskId
    ) {
        ConversationAnalysisResult result = conversationService.getResult(taskId);
        return ResponseEntity.ok(result);
    }
}
