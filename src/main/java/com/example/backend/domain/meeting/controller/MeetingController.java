package com.example.backend.domain.meeting.controller;

import com.example.backend.domain.meeting.dto.SessionCreateRequest;
import com.example.backend.domain.meeting.dto.SessionResponse;
import com.example.backend.domain.meeting.service.MeetingService;
import com.example.backend.domain.meeting.dto.MeetingTokenResponse;
import com.example.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 프론트엔드 접속 허용
@Tag(name = "튜터링 API", description = "튜터링 관련 API입니다.")
public class MeetingController {
    
    private final MeetingService meetingService;

    @Operation(summary = "튜터링 방 생성 API", description = "튜터와 학생의 튜터링 미팅룸을 생성합니다.")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<SessionResponse>> initializeSession(
            @RequestBody(required = false) SessionCreateRequest request) {
        
        // 1. 요청 Body가 없으면 빈 DTO 생성
        if (request == null) {
            request = new SessionCreateRequest();
        }

        try {
            // 2. DTO(request)를 Sevice로 전달
            String createdSessionId = meetingService.createSession(request);

            // 3. 결과 포장: String -> SessionResponse DTO로 변환
            SessionResponse responseData = new SessionResponse(createdSessionId);

            // 4. 최종 응답: ApiResponse로 감싸서 반환
            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }    

    // 토큰 발급 API
    @Operation(summary = "토큰 발급 API", description = "토큰을 발급하고 응답합니다.")
    @PostMapping("/sessions/{sessionId}/connections")
    public ResponseEntity<ApiResponse<MeetingTokenResponse>> createConnection(
            @PathVariable("sessionId") String sessionId) {

        try {
            // 1. 서비스 호출하여 토큰 발급
            String token = meetingService.createToken(sessionId);

            // 2. DTO 포장 (String -> MeetingTokenResponse)
            MeetingTokenResponse responseData = new MeetingTokenResponse(token);

            // 3. 최종 응답: ApiResponse로 감싸서 반환 
            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }
}
