package com.example.backend.global.auth.jwt;

import com.example.backend.global.auth.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 헤더(Header)에서 토큰을 꺼냅니다.
        String token = resolveToken(request);

        // 2. 토큰이 있고, 유효한지 검사합니다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 유효하다면, 인증 정보를 가져옵니다.
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // 4. 스프링 시큐리티에게 알려줍니다. (->Context에 저장)
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Security Context에 '{}' 인증 정보를 저장했습니다", authentication.getName());
        }

        // 5. 다음 필터로 요청을 넘깁니다.
        filterChain.doFilter(request, response);
    }

    // 헤더에서 Bearer 토큰을 추출하는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // "Authorization" 헤더가 존재하고, "Bearer "로 시작하는지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 앞의 "Bearer " 7글자를 자르고 뒤의 토큰만 반환
        }

        return null;
    }
}