package com.example.backend.global.config;

import com.example.backend.global.auth.JwtTokenProvider;
import com.example.backend.global.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 리액트 연동을 위한 CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용할 땐 꺼야 함)
                .csrf(AbstractHttpConfigurer::disable)

                // JWT 사용을 위해 세션 stateless 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Form 로그인 & HttpBasic 해제 (JSON 통신만 할 것이므로)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().authenticated()
//                        .requestMatchers(
//                        "/api/users/signup",
//                        "/swagger-ui/**",
//                        "/v3/api-docs/**"
//                        ).permitAll()
//                )

                // (일단 개발 편의를 위해) 모든 요청 허용 -> 나중에 로그인 로직 짤 때 수정 예정입니다!!
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 5. JWT 필터
                // UsernamePasswordAuthenticationFilter(기본 로그인 필터) 앞에 필터를 배치
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // WebRTC 시그널링과 프론트 연동을 위한 사전 설정입니다
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 통신 허용
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://i14b104.p.ssafy.io"
        ));

        // 2. 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 3. 모든 헤더 허용 (Authorization 등)
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 쿠키/인증 정보 포함 허용 (로그인 유지에 필수)
        configuration.setAllowCredentials(true);

        // 5. 브라우저가 응답 헤더를 읽을 수 있도록 허용 (JWT 토큰 반환 시 필요)
        configuration.setExposedHeaders(List.of("Authorization", "Authorization-refresh"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
