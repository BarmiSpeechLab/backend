package com.example.backend.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching // 캐시 기능 활성화
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 1. 사용할 캐시 이름 등록
        cacheManager.registerCustomCache("analysis_results",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES) // 30분 뒤 자동 삭제 (메모리 보호)
                        .maximumSize(1000) // 최대 1000개까지만 저장
                        .recordStats() // 통계 기능
                        .build()
        );

        // 2. 대화 분석 결과 캐시 등록
        cacheManager.registerCustomCache("conversation_results",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build()
        );

        return cacheManager;
    }
}
