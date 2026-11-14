package com.mfaraujo.financialapi.exception;

import org.springframework.http.HttpStatusCode;

/** A Pluggy rejeitou o {@code clientId}/{@code clientSecret} de um dono. */
public class PluggyAuthenticationException extends PluggyException {

	public PluggyAuthenticationException(String owner, HttpStatusCode pluggyStatus) {
		super(owner, pluggyStatus, "Falha de autenticação na Pluggy para o dono '" + owner + "'");
	}

}
