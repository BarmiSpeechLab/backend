package com.example.backend.domain.curriculum.model;

import com.example.backend.domain.ai.dto.AnalysisRequestDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalysisData {
    private String fullText;
    private List<WordData> wordDetails;

    @Getter @Builder
    public static class WordData {
        private String text;               // 단어 (예: "LIKE")
        private List<PhonemeData> phonemes; // 해당 단어의 정석 음소 리스트
    }

    @Getter @Builder
    public static class PhonemeData {
        private String cpl;   // 정석 ARPAbet (예: "L")
        private String cipa;  // 정석 IPA (예: "l")
        private String ckor;  // 정석 한국어 표기 (예: "ㄹ")
        private String type;  // 음소 타입 (예: "vowel", "liquid")
    }
}
