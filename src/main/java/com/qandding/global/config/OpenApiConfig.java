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
        // CSRF 헤더 스키마
        SecurityScheme xsrf = new SecurityScheme()
                .name("X-XSRF-TOKEN")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER);

        return new OpenAPI()
                .info(new Info().title("QANDDING API").version("v1"))
                .components(new Components()
                        .addSecuritySchemes("xsrf", xsrf))
                .addSecurityItem(new SecurityRequirement().addList("xsrf")); // 전역 적용
    }
}
