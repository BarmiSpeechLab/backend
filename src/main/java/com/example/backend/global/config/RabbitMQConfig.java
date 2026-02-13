package com.example.backend.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    // 인프라 중심 radditMQ 개발 아키텍처

    // 1. 메시지 변환기 (Java 객체 -> JSON 변환)
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 2. RabbitTemplate (실제 전송)
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
    
    // 3. RabbitListener Container Factory (메시지 수신 설정)
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter()); // 받을 때 JSON을 DTO로 변환
        
        // 에러 핸들러 추가 (메시지 처리 중 예외 발생 시 로깅)
        factory.setErrorHandler(new ErrorHandler() {
            @Override
            public void handleError(Throwable t) {
                log.error("========================================");
                log.error("[RabbitMQ 리스너 에러 발생]");
                log.error("[에러 타입] {}", t.getClass().getName());
                log.error("[에러 메시지] {}", t.getMessage());
                log.error("[스택 트레이스]", t);
                log.error("========================================");
            }
        });
        
        log.info("[RabbitMQ Config] Listener Container Factory 초기화 완료");
        return factory;
    }

}
