package com.mfaraujo.financialapi.domain.enums;

/** Tipo de transação. {@link #UNKNOWN} cobre valores que a Pluggy passe a devolver. */
public enum TransactionTypesEnum {

	CREDIT("CREDIT"),
	DEBIT("DEBIT"),
	UNKNOWN("UNKNOWN");

	private final String type;

	TransactionTypesEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	/** Converte o texto da Pluggy. Nulo ou valor não mapeado vira {@link #UNKNOWN}. */
	public static TransactionTypesEnum fromPluggy(String value) {
		for (TransactionTypesEnum candidate : values()) {
			if (candidate != UNKNOWN && candidate.type.equals(value)) {
				return candidate;
			}
		}
		return UNKNOWN;
	}

}
