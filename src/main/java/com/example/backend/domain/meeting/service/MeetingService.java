package com.example.backend.domain.meeting.service;

import com.example.backend.domain.meeting.Entity.Meeting;
import com.example.backend.domain.meeting.repository.MeetingRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.java.client.Connection;
import io.openvidu.java.client.ConnectionProperties;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.CustomObjectInputStream;
import org.springframework.stereotype.Service;
import com.example.backend.domain.meeting.dto.SessionCreateRequest;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeetingService {
    
    private final OpenVidu openVidu; // Bean 주입
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    // 새로운 미팅 데이터 생성
    public Long createEmptyMeeting(Long userId, LocalDateTime dateTime) {
        User user = userRepository.findById(userId).orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));
        return meetingRepository.save(new Meeting(user, dateTime)).getId();
    }

    public String createSession(SessionCreateRequest request) // 1. DTO를 파라미터로 받음
            throws OpenViduJavaClientException, OpenViduHttpException {

        // 2. DTO에서 customSessionId 추출
        String customSessionId = request.getCustomSessionId();

        // 3. 방 설정
        // customSessionId가 있으면 넣고, 없으면 랜덤 생성
        SessionProperties properties = new SessionProperties.Builder()
                .customSessionId(customSessionId)
                .build();

        try {
            // OpenVidu 서버에 세션(회의실) 생성 요청
            Session session = this.openVidu.createSession(properties);
            return session.getSessionId(); // 생성된 세션(방) ID 반환

        } catch (OpenViduHttpException e) {
            // 409 Conflict: 이미 존재하는 세션 ID인 경우
            if (e.getStatus() == 409) {
                // 기존 세션 ID를 그대로 사용하도록 반환
                return customSessionId;
            }
            // 다른 에러는 그대로 던짐
            throw e;
        }
    }

    // 세션 접속 토큰 발급
    public String createToken(String sessionId)
            throws OpenViduJavaClientException, OpenViduHttpException {

        // 1. 현재 열려있는 세션(방) 가져오기
        Session session = this.openVidu.getActiveSession(sessionId);

        // 2. 방이 없으면 예외 처리
        if (session == null) {
            throw new IllegalArgumentException("Session ID not found: " + sessionId);
        }

        // 3. 연결 속성 설정 (기본값 사용)
        ConnectionProperties properties = new ConnectionProperties.Builder().build();

        // 4. 세션(방)에 들어갈 연결(Connection) 생성 요청
        Connection connection = session.createConnection(properties);
        
        // 5. 토큰 반환
        return connection.getToken();
    }

    @Transactional
    public String reserveMeeting(Long meetingId, User student) {
        // 1. DB에서 튜터가 만들어둔 빈 미팅(수업 시간)을 찾음
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // 2. 학생 입장 로직 실행 (내부에서 roomId 생성)
        meeting.joinStudent(student);

        // 3. OpenVidu 세션 생성 (엔티티에서 생성된 roomId를 활용)
        try {
            SessionProperties properties = new SessionProperties.Builder()
                    .customSessionId(meeting.getRoomId()) // 엔티티의 해시값을 세션 ID로 사용
                    .build();

            Session session = openVidu.createSession(properties);
            return session.getSessionId();
        } catch (OpenViduHttpException e) {
            if (e.getStatus() == 409) return meeting.getRoomId();
            // 상세 로그 기록 (개발자가 서버에서 확인용)
            log.error("OpenVidu HTTP 에러 발생: 상태코드 {}, 메시지 {}", e.getStatus(), e.getMessage(), e);
            // 클라이언트에게는 조금 더 구체적인 에러 전달
            throw new CustomException(ErrorCode.OPENVIDU_HTTP_ERROR);

        } catch (OpenViduJavaClientException e) {
            // 클라이언트 라이브러리 자체 에러 (설정 오류 등)
            log.error("OpenVidu 자바 클라이언트 예외: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OPENVIDU_CLIENT_ERROR);
        }
    }
}