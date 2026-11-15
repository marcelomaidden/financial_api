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
	void transactionTypeFromPluggyKnownNullAndUnknown() {
		assertThat(TransactionTypesEnum.fromPluggy("CREDIT")).isEqualTo(TransactionTypesEnum.CREDIT);
		assertThat(TransactionTypesEnum.fromPluggy("DEBIT")).isEqualTo(TransactionTypesEnum.DEBIT);
		assertThat(TransactionTypesEnum.fromPluggy(null)).isEqualTo(TransactionTypesEnum.UNKNOWN);
		assertThat(TransactionTypesEnum.fromPluggy("REFUND")).isEqualTo(TransactionTypesEnum.UNKNOWN);
	}

}
