package com.example.backend.domain.curriculum.controller;

import com.example.backend.domain.curriculum.dto.CurriculumResponse;
import com.example.backend.domain.curriculum.service.CurriculumService;
import com.example.backend.global.dto.ApiResponse;
import com.example.backend.global.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Tag(name = "커리큘럼 API", description = "커리큘럼 데이터 API입니다.")
@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @Operation(summary = "커리큘럼 목록 조회 API", description = "커리큘럼 목록을 조회합니다.")
    @GetMapping("/{type}/{theme}")
    public ResponseEntity<ApiResponse<List<CurriculumResponse>>> getCurriculumList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String type,
            @PathVariable String theme
    ) {

        List<CurriculumResponse> response = curriculumService.getCurriculumList(Long.parseLong(userDetails.getUsername()), type, theme);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커리큘럼 개별 조회 API", description = "개별 커리큘럼을 상세 조회하는 API입니다.")
    @GetMapping("/{curriculumId}")
    public ResponseEntity<ApiResponse<CurriculumResponse>> getCurriculumDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long curriculumId) {

        CurriculumResponse response = curriculumService.getCurriculumDetail(Long.parseLong(userDetails.getUsername()), curriculumId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
