package com.mfaraujo.financialapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfaraujo.financialapi.resource.dto.ApiError;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

class ApiErrorsTest {

	@Test
	void invalidRequestIsBadRequestWithItsMessage() {
		ApiError error = ApiErrors.from(new InvalidRequestException("Informe 'owner' junto com 'accountId'"));

		assertThat(error.status()).isEqualTo(400);
		assertThat(error.error()).isEqualTo("Bad Request");
		assertThat(error.message()).isEqualTo("Informe 'owner' junto com 'accountId'");
	}

	@Test
	void allAccountsFailedIsBadGateway() {
		ApiError error = ApiErrors.from(new AllAccountsFailedException());

		assertThat(error.status()).isEqualTo(502);
		assertThat(error.error()).isEqualTo("Bad Gateway");
		assertThat(error.message()).isEqualTo("A consulta à Pluggy falhou para todas as contas");
	}

	@Test
	void existingMappingsAreUnchanged() {
		assertThat(ApiErrors.from(new UnknownOwnerException("fulano")).status()).isEqualTo(404);
		assertThat(ApiErrors.from(new AllOwnersFailedException()).status()).isEqualTo(502);
		assertThat(ApiErrors.from(new PluggyAuthenticationException("marcelo", HttpStatus.UNAUTHORIZED)).status())
				.isEqualTo(502);
		assertThat(ApiErrors.from(new PluggyException("marcelo", HttpStatus.NOT_FOUND, "x")).status()).isEqualTo(404);
		assertThat(ApiErrors.from(new PluggyException("marcelo", HttpStatus.TOO_MANY_REQUESTS, "x")).status())
				.isEqualTo(429);
		assertThat(ApiErrors.from(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "x")).status())
				.isEqualTo(502);
		assertThat(ApiErrors.from(new ResourceAccessException("timeout")).status()).isEqualTo(504);
		assertThat(ApiErrors.from(new IllegalStateException("x")).status()).isEqualTo(500);
	}

}
