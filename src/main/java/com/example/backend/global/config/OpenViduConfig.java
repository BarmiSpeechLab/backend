package com.example.backend.global.config;

import io.openvidu.java.client.OpenVidu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenViduConfig {

    //application.yml에 적은 URL 가져오기
    @Value("${openvidu.url}")
    private String openviduUrl;

    //application.yml에 적은 비밀번호 가져오기
    @Value("${openvidu.secret}")
    private String openviduSecret;

    @Bean
    public OpenVidu openVidu() {
        // 서버가 켜질 때 OpenVidu 객체 생성
        return new OpenVidu(openviduUrl, openviduSecret);
    }
}
