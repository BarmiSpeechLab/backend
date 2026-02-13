package com.example.backend.global.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ApiLogService apiLogService;
    private final ObjectMapper objectMapper;

    // 모든 컨트롤러 메서드를 대상으로 설정a
    @Around("execution(* com.example.backend..controller..*.*(..)) " +
            "&& !execution(* com.example.backend..controller.AnalysisController.*(..))")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // 1. 요청 정보 추출
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String method = request.getMethod();
        String url = request.getRequestURI();
        
        // 파라미터 파싱 시도 (파싱 가능한 것만 로깅)
        String params;
        try {
            Object[] args = joinPoint.getArgs();
            params = objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            params = "Parameter serialization skipped (Entity or non-serializable object)";
        }

        // 2. 실제 비즈니스 로직 실행
        Object result = joinPoint.proceed();

        // 3. 응답 및 소요 시간 계산
        long duration = System.currentTimeMillis() - start;
        
        // Meeting 엔티티 등 LAZY 로딩 문제가 있는 객체는 파싱 스킵
        String response;
        try {
            response = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            response = "Response serialization skipped (Entity with LAZY references)";
        }

        // 4. 비동기로 로깅 서비스 호출 (API 응답 속도에 영향을 주지 않음)
        apiLogService.saveApiLog(method, url, params, response, duration);

        return result;
    }
}
