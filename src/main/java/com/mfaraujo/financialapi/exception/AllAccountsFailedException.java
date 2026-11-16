package com.mfaraujo.financialapi.exception;

/** Nenhuma consulta de conta teve sucesso, e ao menos uma falhou. Vira 502. */
public class AllAccountsFailedException extends RuntimeException {

	public AllAccountsFailedException() {
		super("A consulta à Pluggy falhou para todas as contas");
	}

}
