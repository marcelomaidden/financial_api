package com.mfaraujo.financialapi.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.mfaraujo.financialapi.config.ApiAccessProperties;
import com.mfaraujo.financialapi.exception.ApiErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Exige o cabeçalho {@code X-API-Key} com a chave configurada em toda requisição, antes de
 * resolver a rota. Sem chave configurada (só em desenvolvimento) a verificação fica desligada.
 * <p>
 * O {@code OPTIONS} passa sem chave, porque o preflight do navegador não a leva. A chave é
 * comparada pelo hash SHA-256 dos dois valores, em tempo constante, e nunca é registrada em
 * log nem devolvida na resposta.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-Key";

	static final String MESSAGE = "Chave de API ausente ou inválida";

	private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

	private final boolean enabled;

	private final byte[] expectedDigest;

	private final JsonMapper jsonMapper;

	public ApiKeyFilter(ApiAccessProperties properties, JsonMapper jsonMapper) {
		this.enabled = properties.apiKeyEnabled();
		this.expectedDigest = enabled ? digest(properties.apiKey()) : new byte[0];
		this.jsonMapper = jsonMapper;
		if (!enabled) {
			log.warn("Chave de API não configurada: a verificação de {} está desligada", HEADER);
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod()) || isValid(request.getHeader(HEADER))) {
			chain.doFilter(request, response);
			return;
		}
		log.debug("Requisição sem chave de API válida: {} {}", request.getMethod(), request.getRequestURI());
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), ApiErrors.of(HttpStatus.UNAUTHORIZED, MESSAGE));
	}

	private boolean isValid(String given) {
		return given != null && MessageDigest.isEqual(digest(given), expectedDigest);
	}

	private static byte[] digest(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponível", ex);
		}
	}

}
