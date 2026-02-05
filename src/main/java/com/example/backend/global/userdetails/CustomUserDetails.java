package com.example.backend.global.userdetails;

import com.example.backend.domain.user.entity.Role;
import com.example.backend.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    //User 엔티티를 시큐리티가 이해할 수 있는 형태로 랩핑
    private final User user;
    // 1. 권한 반환 (Role -> GrantedAuthrity 변환)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey()));
    }

    // 2. 비밀번호 반환
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 3. 사용자 식별자 반환 (PK)
    @Override
    public String getUsername() {
        // ID(LONG)를 String으로 반환
        return String.valueOf(user.getId());
    }
    /**
     * DB PK를 Long 타입으로 바로 반환
     */
    public Long getUserId() {
        return user.getId();
    }

    /**
     * 유저 엔티티 본체 반환
     */
    public User getUserEntity() {
        return user;
    }

    /**
     * 이메일 반환
     */
    public String getEmail() {
        return user.getEmail();
    }
    /**
     * 유저의 Role Enum을 직접 반환
     */
    public Role getRole() {
        return user.getRole();
    }


    // 4. 계정 상태 여부 (지금은 안 쓰니까 모두 true로 설정)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
