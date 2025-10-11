package com.ktb.community.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("커뮤니티 API 목록")
                        .description("rian의 커뮤니티 실습을 위한 API 목록입니다.")
                        .version("v1.0.0")
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("개발용 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("accessTokenAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY) // 1. 타입을 APIKEY로 변경
                                .in(SecurityScheme.In.HEADER)    // 2. 위치는 HEADER로 유지
                                .name("access")                  // 3. 헤더의 실제 이름(Key)을 'access'로 지정
                        )
                );
    }

    @Bean
    public GroupedOpenApi groupedOpenApiV1() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi groupedOpenApiV2() {
        return GroupedOpenApi.builder()
                .group("v2")
                .pathsToMatch("/v2/**")
                .build();
    }
}
