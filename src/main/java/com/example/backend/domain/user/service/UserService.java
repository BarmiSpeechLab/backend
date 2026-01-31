package com.example.backend.domain.user.service;

import com.example.backend.domain.user.dto.SignUpRequest;
import com.example.backend.domain.user.dto.UpdateRequest;
import com.example.backend.domain.user.dto.UserResponse;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.exception.CustomException;
import com.example.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. 내 정보 조회
    public UserResponse getMyInfo(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    // 2. 내 정보 수정
    @Transactional
    public void updateMyInfo(Long id, UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(request.getNickname(), request.getProfileImage());
    }

    // 3. 회원 탈퇴
    @Transactional
    public void withdraw(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);
    }

    // 4. 회원가입
    @Transactional
    public User createUser(SignUpRequest request) {
        // 유저 회원가입 전 email로 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            // 중복아이디 예외처리
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            // 중복닉네임 예외처리
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User user = request.toEntity();
        return userRepository.save(user);
    }

    // 5. 튜토리얼 완료 처리
    @Transactional
    public void completeTutorial(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        user.finishTutorial(); // Entity 메서드 호출 (Dirty Checking으로 자동 저장)
    }
}
