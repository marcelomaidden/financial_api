package com.mfaraujo.financialapi.exception;

import com.mfaraujo.financialapi.resource.dto.ApiError;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

/**
 * Regra única de tradução de falhas em {@link ApiError}. Usada pelo handler
 * global e pela falha parcial por dono, para que as duas respondam igual.
 */
public final class ApiErrors {

	private ApiErrors() {
	}

	public static ApiError from(RuntimeException ex) {
		if (ex instanceof UnknownOwnerException) {
			return of(HttpStatus.NOT_FOUND, ex.getMessage());
		}
		if (ex instanceof PluggyAuthenticationException) {
			return of(HttpStatus.BAD_GATEWAY, ex.getMessage());
		}
		if (ex instanceof PluggyException pluggy) {
			return fromPluggy(pluggy);
		}
		if (ex instanceof ResourceAccessException) {
			return of(HttpStatus.GATEWAY_TIMEOUT, "A Pluggy está indisponível ou não respondeu a tempo");
		}
		if (ex instanceof AllOwnersFailedException || ex instanceof AllAccountsFailedException) {
			return of(HttpStatus.BAD_GATEWAY, ex.getMessage());
		}
		if (ex instanceof InvalidRequestException) {
			return of(HttpStatus.BAD_REQUEST, ex.getMessage());
		}
		return of(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
	}

	private static ApiError fromPluggy(PluggyException ex) {
		int status = ex.getPluggyStatus().value();
		if (status == HttpStatus.NOT_FOUND.value()) {
			String message = ex.getMessage() != null ? ex.getMessage() : "Recurso não encontrado na Pluggy";
			return of(HttpStatus.NOT_FOUND, message);
		}
		if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
			return of(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisições da Pluggy atingido");
		}
		return of(HttpStatus.BAD_GATEWAY, "Erro ao consultar a Pluggy");
	}

	public static ApiError of(HttpStatus status, String message) {
		return new ApiError(status.value(), status.getReasonPhrase(), message);
	}

}
