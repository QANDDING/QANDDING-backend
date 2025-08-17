package com.qandding.global.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class WebCorsConfig {
	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;
	@Value("${app.cors.allowed-methods}")
	private String allowedMethods;
	@Value("${app.cors.allowed-headers}")
	private String allowedHeaders;
	@Value("${app.cors.exposed-headers}")
	private String exposedHeaders;
	@Value("${app.cors.allow-credentials}")
	private boolean allowCredentials;
	@Value("${app.cors.max-age-seconds}")
	private long maxAgeSeconds;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
		config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
		config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
		config.setExposedHeaders(Arrays.asList(exposedHeaders.split(",")));
		config.setAllowCredentials(allowCredentials);
		config.setMaxAge(maxAgeSeconds);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
