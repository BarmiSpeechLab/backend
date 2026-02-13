package com.example.backend.domain.ai.dto;

import lombok.*;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AnalysisRequestDto {
    private Metadata metadata;
    private UserInput userInput;

    private Map<String, Object> guideData;

    @Getter
    @Builder
    @ToString
    public static class Metadata {
        private String requestId;
        private Long userId;
        private Long curriculumId;
        private String timestamp;
    }

    @Getter
    @Builder
    @ToString
    public static class UserInput {
        private String fileName;    // 공유 볼륨 내 파일명
        private String audioFormat; // 확장자
    }
}
