package com.example.backend.domain.report.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String url;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generate(String prompt) {
        // [범인 색출 로그] 실제 자바가 인식하는 키 값 확인
        log.info("========================================");
        log.info("🔑 [KEY CHECK] 키 길이: {}", apiKey.length());
        if (apiKey.length() > 5) {
            log.info("🔑 [KEY CHECK] 앞 4자리: [{}]", apiKey.substring(0, 4));
            log.info("🔑 [KEY CHECK] 뒤 4자리: [{}]", apiKey.substring(apiKey.length() - 4));
        } else {
            log.info("🔑 [KEY CHECK] 전체 값: [{}]", apiKey);
        }
        log.info("========================================");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // [중요] setBearerAuth는 자동으로 앞에 "Bearer "를 붙입니다.
        // 만약 키 값 자체에 "Bearer "가 포함되어 있다면 "Bearer Bearer sk-..."가 되어 401 에러가 납니다.
        headers.setBearerAuth(apiKey); 

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful pronunciation tutor."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map choice = (Map) ((List<?>) response.getBody().get("choices")).get(0);
            Map message = (Map) choice.get("message");
            return message.get("content").toString();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("🚨 AI 요청 실패 (상태 코드: {})", e.getStatusCode());
            log.error("🚨 서버 응답 본문: {}", e.getResponseBodyAsString());
            throw new RuntimeException("AI 서버 에러: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("🚨 알 수 없는 오류: ", e);
            throw new RuntimeException("시스템 오류");
        }
    }
}