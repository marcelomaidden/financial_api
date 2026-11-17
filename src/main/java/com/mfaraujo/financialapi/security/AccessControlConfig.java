package com.mfaraujo.financialapi.security;

import java.util.List;

import com.mfaraujo.financialapi.config.ApiAccessProperties;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Registra os filtros de acesso: primeiro o CORS e depois a chave de API. A ordem importa: se
 * a chave rejeitasse antes, o 401 sairia sem os cabeçalhos de CORS e o navegador mostraria
 * "CORS error" no lugar do 401. Os filtros não são {@code @Component} para o Spring Boot não
 * registrá-los uma segunda vez.
 */
@Configuration
class AccessControlConfig {

	static final int CORS_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	static final int API_KEY_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

	private static final long PREFLIGHT_CACHE_SECONDS = 3600;

	@Bean
	FilterRegistrationBean<CorsFilter> corsFilterRegistration(ApiAccessProperties properties) {
		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter(properties));
		registration.setOrder(CORS_ORDER);
		return registration;
	}

	@Bean
	FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(ApiAccessProperties properties,
			JsonMapper jsonMapper) {
		FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>(
				new ApiKeyFilter(properties, jsonMapper));
		registration.setOrder(API_KEY_ORDER);
		return registration;
	}

	/**
	 * Só as origens listadas. Métodos de leitura, o cabeçalho da chave e os que um cliente HTTP
	 * costuma mandar, sem credenciais. Sem origem listada, nenhuma é permitida.
	 */
	static CorsFilter corsFilter(ApiAccessProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.cors().allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(ApiKeyFilter.HEADER, "Content-Type", "Accept"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(PREFLIGHT_CACHE_SECONDS);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return new CorsFilter(source);
	}

}
