package com.example.backend.domain.meeting.repository;

import com.example.backend.domain.meeting.Entity.Meeting;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    // 특정 튜터가 해당 시간에 이미 생성한 미팅이 있는지 확인
    boolean existsByTutorAndDatetime(User tutor, LocalDateTime datetime);
    
    // 특정 튜터의 예약 가능한 일정 조회 (tutee가 null인 경우)
    List<Meeting> findByTutorIdAndTuteeIsNull(Long tutorId);

    // 특정 튜티의 예약 일정 조회
    List<Meeting> findByTuteeId(Long tuteeId);
}
