package com.mfaraujo.financialapi.domain.enums;

/** Tipo de conta. {@link #UNKNOWN} cobre valores que a Pluggy passe a devolver. */
public enum AccountTypesEnum {

	BANK("BANK"),
	CREDIT("CREDIT"),
	UNKNOWN("UNKNOWN");

	private final String type;

	AccountTypesEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	/** Converte o texto da Pluggy. Nulo ou valor não mapeado vira {@link #UNKNOWN}. */
	public static AccountTypesEnum fromPluggy(String value) {
		for (AccountTypesEnum candidate : values()) {
			if (candidate != UNKNOWN && candidate.type.equals(value)) {
				return candidate;
			}
		}
		return UNKNOWN;
	}

}
