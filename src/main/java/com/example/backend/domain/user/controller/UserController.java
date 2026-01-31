package com.example.backend.domain.user.controller;

import com.example.backend.domain.user.dto.SignUpRequest;
import com.example.backend.domain.user.dto.UpdateRequest;
import com.example.backend.domain.user.dto.UserResponse;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.auth.dto.LoginRequest;
import com.example.backend.global.auth.dto.TokenResponse;
import com.example.backend.global.auth.service.AuthService;
import com.example.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "유저 기능 API", description = "유저 데이터 관리용 API입니다")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "회원가입 API", description = "회원가입을 요청합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody SignUpRequest signUpRequest) {
        userService.createUser(signUpRequest);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
    }

    @Operation(summary = "로그인 API", description = "로그인을 요청합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "로그아웃 API", description = "리프레시 토큰 미구현으로 인해 아직 개발중인 API입니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername()); // Username이 email이라고 가정
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @Operation(summary = "유저 정보 조회 API", description = "유저 정보를 요청합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getMyInfo(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "유저 정보 수정 API", description = "유저의 정보 수정을 요청합니다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<String>> updateMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateRequest request) {
        userService.updateMyInfo(Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.ok(ApiResponse.success("정보 수정 완료"));
    }

    @Operation(summary = "회원 탈퇴 API", description = "회원탈퇴를 요청합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<String>> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        userService.withdraw(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴 완료"));
    }
    @Operation(summary = "온보딩 완료 처리 API", description = "튜토리얼 완료 후 온보딩 요청합니다.")
    @PatchMapping("/tutorial")
    public ResponseEntity<ApiResponse<String>> completeTutorial(@AuthenticationPrincipal UserDetails userDetails) {
        userService.completeTutorial(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.ok(ApiResponse.success("온보딩 완료 처리 성공"));
    }
}
