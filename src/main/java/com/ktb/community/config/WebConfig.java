package com.ktb.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 👇 여러 개의 출처를 쉼표로 구분하여 추가
                .allowedOrigins("http://127.0.0.1:3000",  "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // OPTIONS 추가
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
