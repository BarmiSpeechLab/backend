package com.example.backend.domain.meeting.controller;

import com.example.backend.domain.meeting.Entity.Meeting;
import com.example.backend.domain.meeting.dto.MeetingJoinResponse;
import com.example.backend.domain.meeting.dto.SessionCreateRequest;
import com.example.backend.domain.meeting.dto.SessionResponse;
import com.example.backend.domain.meeting.service.MeetingService;
import com.example.backend.domain.meeting.dto.MeetingTokenResponse;
import com.example.backend.domain.user.entity.User;
import com.example.backend.global.dto.ApiResponse;
import com.example.backend.global.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 프론트엔드 접속 허용
@Tag(name = "튜터링 API", description = "튜터링 관련 API입니다.")
public class MeetingController {
    
    private final MeetingService meetingService;

    @PostMapping("")
    @PreAuthorize("hasRole('TUTOR')")   // api 요청 시 튜터인지 권한 확인
    @Operation(summary = "튜터의 Meeting 생성 API", description = "튜터가 가능한 시간대에 미팅 데이터를 생성합니다.")
    public ResponseEntity<ApiResponse<Long>> createMeetingByTutor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody LocalDateTime dateTime) {
        // 튜터 정보와 시간을 받아 Meeting 엔티티 저장 로직 호출
        Long meetingId = meetingService.createEmptyMeeting(userDetails.getUserId(), dateTime);
        return ResponseEntity.ok(ApiResponse.success(meetingId));
    }

    @PostMapping("/{meetingId}/join")
    @Operation(summary = "학생의 튜터링 참여 API", description = "튜터의 미팅 시간이 활성화되면 해당 미팅에 참여요청을 합니다.")
    public ResponseEntity<ApiResponse<MeetingJoinResponse>> joinMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails student) { // 현재 로그인한 학생 정보
        
        // DB 예약 및 OpenVidu 세션 생성
        String sessionId = meetingService.reserveMeeting(meetingId, student.getUserEntity());
        // 해당 세션에 접속할 수 있는 토큰 발급
        String token = meetingService.createToken(sessionId);

        return ResponseEntity.ok(ApiResponse.success(new MeetingJoinResponse(sessionId, token)));
    }

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
