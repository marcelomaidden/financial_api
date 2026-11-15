package com.mfaraujo.financialapi.domain.enums;

/** Moedas que a API trata de forma especial. O código de moeda nos modelos continua sendo texto. */
public enum CurrencyEnum {

	BRL("BRL");

	private final String type;

	CurrencyEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

}
