package com.mfaraujo.financialapi.exception;

import org.springframework.http.HttpStatusCode;

/** Erro devolvido pela Pluggy para uma chamada feita em nome de um dono. */
public class PluggyException extends RuntimeException {

	private final String owner;

	private final HttpStatusCode pluggyStatus;

	public PluggyException(String owner, HttpStatusCode pluggyStatus, String message) {
		super(message);
		this.owner = owner;
		this.pluggyStatus = pluggyStatus;
	}

	public String getOwner() {
		return owner;
	}

	public HttpStatusCode getPluggyStatus() {
		return pluggyStatus;
	}

}
