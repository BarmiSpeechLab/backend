//package com.example.backend.domain.meeting.Entity;
//
//import com.example.backend.domain.user.entity.User;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//import java.util.Date;
//
//@Entity
//@Getter
//@Builder
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor
//@Table(name = "scheduls")
//public class Schedule {
//
//    @Id
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    // 포맷 : 2023-04-10T15:30:00
//    // 위 형태에서 datetime을 나눠서
//    private LocalDateTime datetime;
//}
