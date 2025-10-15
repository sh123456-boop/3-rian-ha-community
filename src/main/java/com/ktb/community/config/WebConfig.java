package com.ktb.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins("http://127.0.0.1:5500") // 이 출처의 요청을 허용
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 쿠키 인증 요청 허용
                .maxAge(3600); // 프리플라이트 요청 캐시 시간(초)
    }
}
