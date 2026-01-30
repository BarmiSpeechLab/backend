package com.example.backend.domain.user.entity;

import com.example.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "USERS")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Builder.Default
    private Boolean isTutorialFinished = false; // 튜토리얼 완료 여부

    private String profileImgUrl;   // 프로필 이미지 url

    // 회원 정보 수정 비즈니스 로직
    public void updateProfile(String nickname, String profileImage) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImage != null && !profileImage.isBlank()) {
            this.profileImgUrl = profileImage;
        }
    }
}
