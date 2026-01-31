package com.example.backend.domain.curriculum;

import com.example.backend.domain.curriculum.dto.CurriculumListResponse;
import com.example.backend.domain.curriculum.service.CurriculumService;
import com.example.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CurriculumListResponse>>> getCurriculumList(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CurriculumListResponse> response = curriculumService.getCurriculumList(Long.parseLong(userDetails.getUsername()));

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
