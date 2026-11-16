package com.mfaraujo.financialapi.exception;

/** A combinação de parâmetros da requisição não é válida. Vira 400. */
public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}

}
