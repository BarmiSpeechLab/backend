package com.example.backend.domain.report.ai.service;

import com.example.backend.domain.report.ai.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmReportService {

    private final OpenAiClient openAiClient;

    public String generateIpaReport(String prompt) {
        return openAiClient.generate(prompt);
    }
}