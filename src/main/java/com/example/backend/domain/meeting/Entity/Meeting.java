package com.example.backend.domain.meeting.Entity;

import com.example.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "meetings")
public class Meeting {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "web_rtc_room_id")
    private String roomId; // WebRTC 연결용 해시 ID (학생 입장 시 생성)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutee_id", nullable = false)
    private User tutee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private User tutor;

    @Column(nullable = false)
    private LocalDateTime datetime; // 수업 시간 (포맷 : 2023-04-10T15:30:00)
    private boolean isClosed;       // 수업 종료 여부


    //===============================
    // 데이터 수정 메서드
    //===============================

    // 1. 튜터가 빈 방 생성 (학생X, RoomID 없음)
    @Builder
    public Meeting(User tutor, LocalDateTime datetime) {
        this.tutor = tutor;
        this.datetime = datetime;
        this.isClosed = false;
        this.tutee = null;
        this.roomId = null;
    }
    // 2. 학생이 입장(예약)하는 비즈니스 로직
    // 호출되는 순간 RoomID가 만들어집니다.
    public void joinStudent(User student) {
        if (this.tutee != null) {
            throw new IllegalStateException("이미 예약된 수업입니다.");
        }
        this.tutee = student;
        // 학생이 들어오고 Tutor+Student+Time -> 해시 생성
        this.roomId = generateRoomHash(this.tutor.getId(), student.getId(), this.datetime);
    }

    // 3. 수업 종료
    // 종료된 수업을 대상으로 수업 요약같은 리포트를 전송해주고 데이터는 삭제하면 좋을 것 같아요
    public void closeMeeting() {
        this.isClosed = true;
    }

    // 해시 생성 로직
    private String generateRoomHash(Long tutorId, Long studentId, LocalDateTime time) {
        String rawKey = String.format("%d-%d-%s", tutorId, studentId, time.toString());
        return UUID.nameUUIDFromBytes(rawKey.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
