package com.example.backend.global.auth;

import com.example.backend.global.userdetails.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${jwt.secret}")
    private String secretKey;                           // jwt 암호화 키
    @Value("${jwt.access-token-validity-in-seconds}")
    private long accessTokenValidityInMilliseconds;     // access token 만료시간
    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInMilliseconds;    // refresh token 만료시간

    private SecretKey key;

    // 1. 초기화
    @PostConstruct
    public void init() {
        // 환경변수의 암호화 키를 바이트 배열로 변환 후 secret key 객체를 생성합니다
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        // 밀리초 단위로 변환 (*1000)
        this.accessTokenValidityInMilliseconds *= 1000;
        this.refreshTokenValidityInMilliseconds *= 1000;
    }

    // 2. 토큰 생성
    // 공통
    public String createToken(Authentication authentication, long validity) {
        // 로그인 성공 후
        // payload를 위해 리스트 형태의 객체 권한들을 쉼표로 이어진 문자열로 합치는 코드입니다.
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        long now = (new Date()).getTime();
        Date validityDate = new Date(now + validity);

        // 로그인 시 Principal이 userdetails인지 아닌지 주의
        String userId = authentication.getName();
        return Jwts.builder()
                .subject(userId)
                .claim("auth",authorities)
                .issuedAt(new Date((now)))
                .expiration(validityDate)
                .signWith(key)
                .compact();
    }
    // 액세스 토큰
    public String createAccessToken(Authentication authentication) {
        return createToken(authentication, accessTokenValidityInMilliseconds);
    }

    // 리프레시 토큰
    public String createRefreshToken(Authentication authentication) {
        return createToken(authentication, refreshTokenValidityInMilliseconds);
    }

    // 3. 토큰 정보 추출 & 인증 객체 생성

    // 토큰에서 인증 정보(Authentication) 조회 -> 필터에서 사용
    public Authentication getAuthentication(String token) {
        // 토큰에서 Claims(내용물) 추출
        Claims claims = parseClaims(token);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 권한 정보 추출
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // 토큰 subject(PK)로 DB에서 유저 조회 (loadUserById 사용!)
        Long userId = Long.parseLong(claims.getSubject());
        UserDetails userDetails = customUserDetailsService.loadUserById(userId);

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    // 토큰 복호화 (내부 사용)
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key) // 서명 검증
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims(); // 만료되어도 정보는 확인 가능하도록
        }
    }

    // 4. 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

}
