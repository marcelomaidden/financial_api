package com.mfaraujo.financialapi.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Injeta o {@code X-API-KEY} do dono em cada chamada. Se a Pluggy responder 401,
 * invalida a chave desse dono e repete a chamada uma única vez.
 */
class PluggyApiKeyInterceptor implements ClientHttpRequestInterceptor {

	static final String API_KEY_HEADER = "X-API-KEY";

	private static final Logger log = LoggerFactory.getLogger(PluggyApiKeyInterceptor.class);

	private final String owner;

	private final PluggyApiKeyProvider apiKeyProvider;

	PluggyApiKeyInterceptor(String owner, PluggyApiKeyProvider apiKeyProvider) {
		this.owner = owner;
		this.apiKeyProvider = apiKeyProvider;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		String apiKey = apiKeyProvider.getApiKey(owner);
		request.getHeaders().set(API_KEY_HEADER, apiKey);
		ClientHttpResponse response = execution.execute(request, body);
		if (response.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
			return response;
		}
		response.close();
		log.debug("Pluggy respondeu 401 para o dono '{}'; renovando a apiKey e repetindo", owner);
		apiKeyProvider.invalidate(owner, apiKey);
		request.getHeaders().set(API_KEY_HEADER, apiKeyProvider.getApiKey(owner));
		return execution.execute(request, body);
	}

}
