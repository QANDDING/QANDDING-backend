package com.qandding.config;

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
		SecurityScheme cookieAuth = new SecurityScheme()
			.name("access_token")
			.type(SecurityScheme.Type.APIKEY)
			.in(SecurityScheme.In.COOKIE);
		return new OpenAPI()
			.info(new Info().title("QANDDING API").version("v1"))
			.components(new Components().addSecuritySchemes("cookieAuth", cookieAuth))
			.addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
	}
}
