package com.mfaraujo.financialapi.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnumsTest {

	@Test
	void accountTypeFromPluggyKnownNullAndUnknown() {
		assertThat(AccountTypesEnum.fromPluggy("BANK")).isEqualTo(AccountTypesEnum.BANK);
		assertThat(AccountTypesEnum.fromPluggy("CREDIT")).isEqualTo(AccountTypesEnum.CREDIT);
		assertThat(AccountTypesEnum.fromPluggy(null)).isEqualTo(AccountTypesEnum.UNKNOWN);
		assertThat(AccountTypesEnum.fromPluggy("INVESTMENT")).isEqualTo(AccountTypesEnum.UNKNOWN);
		assertThat(AccountTypesEnum.fromPluggy("UNKNOWN")).isEqualTo(AccountTypesEnum.UNKNOWN);
	}

	@Test
	void loanKindFromPluggyKnownNullAndUnknown() {
		assertThat(LoanKindEnum.fromPluggy("LOAN")).isEqualTo(LoanKindEnum.LOAN);
		assertThat(LoanKindEnum.fromPluggy("FINANCING")).isEqualTo(LoanKindEnum.FINANCING);
		assertThat(LoanKindEnum.fromPluggy("INVOICE_FINANCING")).isEqualTo(LoanKindEnum.INVOICE_FINANCING);
		assertThat(LoanKindEnum.fromPluggy("UNARRANGED_ACCOUNT_OVERDRAFT"))
				.isEqualTo(LoanKindEnum.UNARRANGED_ACCOUNT_OVERDRAFT);
		assertThat(LoanKindEnum.fromPluggy(null)).isEqualTo(LoanKindEnum.UNKNOWN);
		assertThat(LoanKindEnum.fromPluggy("LEASING")).isEqualTo(LoanKindEnum.UNKNOWN);
	}

	@Test
	void transactionTypeFromPluggyKnownNullAndUnknown() {
		assertThat(TransactionTypesEnum.fromPluggy("CREDIT")).isEqualTo(TransactionTypesEnum.CREDIT);
		assertThat(TransactionTypesEnum.fromPluggy("DEBIT")).isEqualTo(TransactionTypesEnum.DEBIT);
		assertThat(TransactionTypesEnum.fromPluggy(null)).isEqualTo(TransactionTypesEnum.UNKNOWN);
		assertThat(TransactionTypesEnum.fromPluggy("REFUND")).isEqualTo(TransactionTypesEnum.UNKNOWN);
	}

}
