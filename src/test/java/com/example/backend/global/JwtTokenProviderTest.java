package com.example.backend.global;

import com.example.backend.domain.user.entity.Role;
import com.example.backend.domain.user.entity.User;
import com.example.backend.global.auth.JwtTokenProvider;
import com.example.backend.global.userdetails.CustomUserDetails;
import com.example.backend.global.userdetails.CustomUserDetailsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    // 테스트용 임시 비밀키 (Base64 Encoded)
    private final String secretKey = "c3ByaW5nLWJvb3Qtc2VjdXJpdHktand0LXR1dG9yaWFsLWppd29vbi1zcHJpbmctYm9vdC1zZWN1cml0eS1qd3QtdHV0b3JpYWwK";
    private final long accessTokenValidity = 1800;
    private final long refreshTokenValidity = 86400;

    @BeforeEach
    void setUp() {
        // @Value 어노테이션이 붙은 필드에 리플렉션으로 값을 주입
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInMilliseconds", accessTokenValidity);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidityInMilliseconds", refreshTokenValidity);

        // @PostConstruct 메서드 수동 호출
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("토큰 생성 및 검증 테스트")
    void generateAndValidateToken() {
        // given
        Long userId = 1L;
        String role = "ROLE_USER";

        CustomUserDetails customUserDetails = new CustomUserDetails(
                User.builder()
                        .id(userId)
                        .email("test@ssafy.com")
                        .password("password")
                        .role(Role.USER)
                        .build()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails, "", Collections.singletonList(new SimpleGrantedAuthority(role))
        );

        // when
        String accessToken = jwtTokenProvider.createAccessToken(authentication);

        // then
        System.out.println("Generated Token: " + accessToken);
        assertThat(accessToken).isNotNull();
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
    }

    @Test
    @DisplayName("토큰에서 회원 ID(PK) 추출 테스트")
    void getUserIdFromToken() {
        // given
        Long userId = 15L;

        // 직접 토큰 생성 (Provider 로직 역검증)
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("auth", "ROLE_USER")
                .signWith(key)
                .compact();

        // Mocking: loadUserById 호출 시 가짜 객체 반환
        User mockUser = User.builder().id(userId).role(Role.USER).email("test@ssafy.com").password("pw").build();
        given(customUserDetailsService.loadUserById(userId))
                .willReturn(new CustomUserDetails(mockUser));

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // then
        assertThat(userDetails.getUserId()).isEqualTo(String.valueOf(userId));
        System.out.println("Extracted User ID: " + userDetails.getUserId());
    }
}