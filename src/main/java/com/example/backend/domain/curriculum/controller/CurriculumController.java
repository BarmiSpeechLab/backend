package com.example.backend.domain.curriculum.controller;

import com.example.backend.domain.curriculum.dto.CurriculumResponse;
import com.example.backend.domain.curriculum.service.CurriculumService;
import com.example.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Tag(name = "커리큘럼 API", description = "커리큘럼 데이터 API입니다.")
@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CurriculumResponse>>> getCurriculumList(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CurriculumResponse> response = curriculumService.getCurriculumList(Long.parseLong(userDetails.getUsername()));

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
