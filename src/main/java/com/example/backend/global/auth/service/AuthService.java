package com.example.backend.global.auth.service;

import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.auth.JwtTokenProvider;
import com.example.backend.global.auth.dto.LoginRequest;
import com.example.backend.global.auth.dto.TokenResponse;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import com.example.backend.global.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. 이메일로 유저 찾기 (없으면 에러)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        // 2. 비밀번호 확인
        // matches(입력값, 암호화된값) 순서 중요!
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 인증 객체(Authentication) 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );

        // 4. 토큰 발급 (Access + Refresh)
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // 5. 응답 객체 빌드 후 반환
        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(3600L) // 1시간 (프론트 참고용)
                .build();
    }

    // 4. 로그아웃
    @Transactional
    public void logout(Long id) {
        /* TODO
        *   저장한 리프레시 토큰을 삭제하는 기능을 구현할 예정입니다.
        *   캐시 구현 코드가 아직 머지되지 않아서 추후 구현 예정.
        * */
    }
}
