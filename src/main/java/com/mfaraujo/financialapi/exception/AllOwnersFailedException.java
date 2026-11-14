package com.mfaraujo.financialapi.exception;

/** A consulta sem filtro de dono falhou para todos os donos. */
public class AllOwnersFailedException extends RuntimeException {

	public AllOwnersFailedException() {
		super("A consulta à Pluggy falhou para todos os donos");
	}

}
