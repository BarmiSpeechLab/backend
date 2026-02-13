package com.example.backend.domain.user.dto;

import com.example.backend.domain.user.entity.Role;
import com.example.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {
    private String password;
    private String nickname;
    private String email;
    private Role role;

    public User toEntity() {
        return User.builder()
                .password(password)
                .nickname(nickname)
                .email(email)
                .role(role)
                .build();
    }
}
