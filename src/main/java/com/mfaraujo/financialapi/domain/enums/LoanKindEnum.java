package com.mfaraujo.financialapi.domain.enums;

/** Família do empréstimo. {@link #UNKNOWN} cobre valores que a Pluggy passe a devolver. */
public enum LoanKindEnum {

	LOAN("LOAN"),
	FINANCING("FINANCING"),
	INVOICE_FINANCING("INVOICE_FINANCING"),
	UNARRANGED_ACCOUNT_OVERDRAFT("UNARRANGED_ACCOUNT_OVERDRAFT"),
	UNKNOWN("UNKNOWN");

	private final String type;

	LoanKindEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	/** Converte o texto da Pluggy. Nulo ou valor não mapeado vira {@link #UNKNOWN}. */
	public static LoanKindEnum fromPluggy(String value) {
		for (LoanKindEnum candidate : values()) {
			if (candidate != UNKNOWN && candidate.type.equals(value)) {
				return candidate;
			}
		}
		return UNKNOWN;
	}

}
