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
        // [개선] MultipartFile 등 직렬화 불가능한 객체 필터링
        Object[] args = joinPoint.getArgs();
        List<Object> filteredArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof org.springframework.web.multipart.MultipartFile) {
                filteredArgs.add("File: " + ((org.springframework.web.multipart.MultipartFile) arg).getOriginalFilename());
            } else if (arg instanceof jakarta.servlet.http.HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                continue; // 서블릿 객체는 로깅에서 제외
            } else {
                filteredArgs.add(arg);
            }
        }

        String params = objectMapper.writeValueAsString(joinPoint.getArgs());

        // 2. 실제 비즈니스 로직 실행
        Object result = joinPoint.proceed();

        // 3. 응답 및 소요 시간 계산
        long duration = System.currentTimeMillis() - start;
        String response = objectMapper.writeValueAsString(result);

        // 4. 비동기로 로깅 서비스 호출 (API 응답 속도에 영향을 주지 않음)
        apiLogService.saveApiLog(method, url, params, response, duration);

        return result;
    }
}
