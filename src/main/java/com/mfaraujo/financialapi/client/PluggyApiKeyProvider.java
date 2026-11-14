package com.mfaraujo.financialapi.client;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mfaraujo.financialapi.client.dto.PluggyAuthRequest;
import com.mfaraujo.financialapi.client.dto.PluggyAuthResponse;
import com.mfaraujo.financialapi.config.PluggyProperties;
import com.mfaraujo.financialapi.exception.PluggyAuthenticationException;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Obtém e guarda em memória a {@code apiKey} da Pluggy de cada dono. A chave é
 * renovada antes de expirar ou quando invalidada após um 401. A renovação é
 * sincronizada por dono: dois donos nunca bloqueiam um ao outro.
 */
@Component
public class PluggyApiKeyProvider {

	private static final Logger log = LoggerFactory.getLogger(PluggyApiKeyProvider.class);

	/** Validade assumida quando a chave não informa {@code exp} (Pluggy: 2 horas). */
	static final Duration FALLBACK_TTL = Duration.ofMinutes(110);

	/** Renova a chave com esta antecedência em relação à expiração. */
	static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);

	private final PluggyProperties properties;

	private final RestClient authClient;

	private final JsonMapper jsonMapper;

	private final Clock clock;

	private final Map<String, CachedKey> cache = new ConcurrentHashMap<>();

	private final Map<String, Object> locks = new ConcurrentHashMap<>();

	public PluggyApiKeyProvider(PluggyProperties properties, RestClient.Builder restClientBuilder,
			PluggyHttpRequestFactory requestFactory, JsonMapper jsonMapper) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		this.clock = Clock.systemUTC();
		this.authClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory.get())
				.build();
	}

	public String getApiKey(String owner) {
		CachedKey cached = cache.get(owner);
		if (cached != null && cached.isValid(clock.instant())) {
			return cached.apiKey();
		}
		synchronized (locks.computeIfAbsent(owner, key -> new Object())) {
			cached = cache.get(owner);
			if (cached != null && cached.isValid(clock.instant())) {
				return cached.apiKey();
			}
			CachedKey fresh = authenticate(owner);
			cache.put(owner, fresh);
			return fresh.apiKey();
		}
	}

	/** Descarta a chave do dono, forçando nova autenticação na próxima chamada. */
	public void invalidate(String owner, String rejectedKey) {
		cache.computeIfPresent(owner, (key, cached) -> cached.apiKey().equals(rejectedKey) ? null : cached);
	}

	private CachedKey authenticate(String ownerName) {
		PluggyProperties.Owner owner = properties.findOwner(ownerName)
				.orElseThrow(() -> new UnknownOwnerException(ownerName));
		PluggyAuthResponse response = authClient.post()
				.uri("/auth")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new PluggyAuthRequest(owner.clientId(), owner.clientSecret()))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, resp) -> {
					HttpStatusCode status = resp.getStatusCode();
					if (status.value() == HttpStatus.UNAUTHORIZED.value()
							|| status.value() == HttpStatus.FORBIDDEN.value()
							|| status.value() == HttpStatus.BAD_REQUEST.value()) {
						throw new PluggyAuthenticationException(ownerName, status);
					}
					throw new PluggyException(ownerName, status, "Erro ao autenticar na Pluggy");
				})
				.body(PluggyAuthResponse.class);
		if (response == null || response.apiKey() == null || response.apiKey().isBlank()) {
			throw new PluggyException(ownerName, HttpStatus.BAD_GATEWAY, "A Pluggy não devolveu apiKey");
		}
		Instant expiresAt = expirationOf(response.apiKey());
		log.debug("apiKey da Pluggy obtida para o dono '{}', expira em {}", ownerName, expiresAt);
		return new CachedKey(response.apiKey(), expiresAt.minus(REFRESH_MARGIN));
	}

	/** Lê {@code exp} do JWT; se não for possível, usa {@link #FALLBACK_TTL}. */
	private Instant expirationOf(String apiKey) {
		try {
			String[] parts = apiKey.split("\\.");
			if (parts.length >= 2) {
				byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
				var exp = jsonMapper.readTree(new String(payload, StandardCharsets.UTF_8)).get("exp");
				if (exp != null && exp.isNumber()) {
					return Instant.ofEpochSecond(exp.asLong());
				}
			}
		}
		catch (RuntimeException ex) {
			log.debug("Não foi possível ler a expiração da apiKey; usando validade padrão");
		}
		return clock.instant().plus(FALLBACK_TTL);
	}

	private record CachedKey(String apiKey, Instant refreshAt) {

		boolean isValid(Instant now) {
			return now.isBefore(refreshAt);
		}

	}

}
