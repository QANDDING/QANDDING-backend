package com.qandding.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // API 문서의 제목과 버전 설정
        Info info = new Info()
                .title("QANDDING API")
                .version("v1")
                .description("Q&Ding 프로젝트 API 명세서");

        // JWT 인증을 위한 SecurityScheme 설정 (Bearer Auth)
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // HTTP 기반의 인증 스킴
                .scheme("bearer")               // Bearer 토큰 스킴 사용
                .bearerFormat("JWT")            // 토큰 형식은 JWT
                .in(SecurityScheme.In.HEADER)   // 토큰은 헤더에 위치
                .name("Authorization");         // 헤더의 이름은 "Authorization"

        // SecurityScheme을 전역적으로 적용하기 위한 SecurityRequirement 설정
        // "bearerAuth"는 위에서 addSecuritySchemes에 사용한 키와 일치해야 함
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth))
                .addSecurityItem(securityRequirement);
    }
}