package com.example.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info; // 👈 이 import가 필요합니다!
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // 보안 스키마 이름 (내부 식별자)
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // 1. API 정보 설정
                .info(new Info()
                        .title("바르미 API 명세서")
                        .description("청각장애인을 위한 영어 발음 학습 서비스 바르미")
                        .version("v1")
                )
                // 2. 보안 스키마 등록 (JWT 설정)
                // API를 사용하기 위한 인증방식을 정의합니다
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                // 3. 모든 API에 보안 스키마 적용
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}