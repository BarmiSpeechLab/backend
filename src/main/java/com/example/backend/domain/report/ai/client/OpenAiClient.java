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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // GMS API는 대부분 Bearer 인증을 사용합니다. 분기 없이 Bearer를 붙여보세요.
        headers.set("Authorization", "Bearer " + apiKey);

        // 로그로 키가 잘 들어갔는지 한 번 더 확인 (보안상 앞자리만)
        log.info("AI 요청 헤더 확인: {}", headers.getFirst("Authorization").substring(0, 15) + "...");
        
//        // SSAFY GMS API 키는 Bearer 없이 바로 Authorization 헤더에 넣는 경우가 많습니다.
//        if (apiKey != null && apiKey.startsWith("S")) {
//            headers.set("Authorization", apiKey);
//        } else {
//            headers.setBearerAuth(apiKey);
//        }

        log.info("AI 요청 시도 - URL: {}, Key prefix: {}", url, 
            (apiKey != null && apiKey.length() > 5) ? apiKey.substring(0, 5) : "invalid");

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
            // 401 에러 시 사용자에게 더 상세한 정보를 줄 수도 있지만, 일단은 인증 오류임을 명시
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("AI 서버 인증 에러: API 키가 잘못되었거나 만료되었습니다.");
            }
            throw new RuntimeException("AI 서버 에러: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("🚨 알 수 없는 오류: ", e);
            throw new RuntimeException("시스템 오류: " + e.getMessage());
        }
    }
}