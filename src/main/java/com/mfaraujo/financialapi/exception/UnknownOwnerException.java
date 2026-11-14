package com.mfaraujo.financialapi.exception;

/** O dono informado não está na configuração {@code pluggy.owners}. */
public class UnknownOwnerException extends RuntimeException {

	public UnknownOwnerException(String owner) {
		super("Dono '" + owner + "' não está configurado");
	}

}
