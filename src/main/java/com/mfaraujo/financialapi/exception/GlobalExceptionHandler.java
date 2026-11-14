package com.mfaraujo.financialapi.exception;

import com.mfaraujo.financialapi.resource.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduz exceções em respostas {@code {status, error, message}}. Nunca inclui
 * credenciais, chaves ou detalhes internos nas mensagens.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
		return respond(ApiErrors.of(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' inválido"));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException ex) {
		return respond(ApiErrors.of(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getParameterName() + "' obrigatório"));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiError> validation(HandlerMethodValidationException ex) {
		return respond(ApiErrors.of(HttpStatus.BAD_REQUEST, "Parâmetros inválidos"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> noResource(NoResourceFoundException ex) {
		return respond(ApiErrors.of(HttpStatus.NOT_FOUND, "Recurso não encontrado"));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiError> runtime(RuntimeException ex) {
		ApiError error = ApiErrors.from(ex);
		if (error.status() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
			log.error("Erro ao processar a requisição", ex);
		}
		else if (error.status() >= 500) {
			log.warn("Falha ao consultar a Pluggy: {} {}", error.status(), error.message());
		}
		return respond(error);
	}

	private static ResponseEntity<ApiError> respond(ApiError error) {
		return ResponseEntity.status(error.status()).body(error);
	}

}
