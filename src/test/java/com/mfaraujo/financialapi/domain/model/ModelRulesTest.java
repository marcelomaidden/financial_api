package com.mfaraujo.financialapi.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import org.junit.jupiter.api.Test;

class ModelRulesTest {

	@Test
	void brlBankAccountNeedsBankTypeBrlAndBalance() {
		assertThat(account(AccountTypesEnum.BANK, "BRL", "10").isBrlBankAccount()).isTrue();
		assertThat(account(AccountTypesEnum.CREDIT, "BRL", "10").isBrlBankAccount()).isFalse();
		assertThat(account(AccountTypesEnum.UNKNOWN, "BRL", "10").isBrlBankAccount()).isFalse();
		assertThat(account(AccountTypesEnum.BANK, "USD", "10").isBrlBankAccount()).isFalse();
		assertThat(account(AccountTypesEnum.BANK, null, "10").isBrlBankAccount()).isFalse();
		assertThat(account(AccountTypesEnum.BANK, "BRL", null).isBrlBankAccount()).isFalse();
	}

	@Test
	void creditCardIsCreditType() {
		assertThat(account(AccountTypesEnum.CREDIT, "BRL", "1").isCreditCard()).isTrue();
		assertThat(account(AccountTypesEnum.BANK, "BRL", "1").isCreditCard()).isFalse();
	}

	@Test
	void brlCreditNeedsCreditTypeBrlAndAmount() {
		assertThat(tx(TransactionTypesEnum.CREDIT, "BRL", "5").isBrlCredit()).isTrue();
		assertThat(tx(TransactionTypesEnum.DEBIT, "BRL", "5").isBrlCredit()).isFalse();
		assertThat(tx(TransactionTypesEnum.UNKNOWN, "BRL", "5").isBrlCredit()).isFalse();
		assertThat(tx(TransactionTypesEnum.CREDIT, "USD", "5").isBrlCredit()).isFalse();
		assertThat(tx(TransactionTypesEnum.CREDIT, "BRL", null).isBrlCredit()).isFalse();
	}

	private static Account account(AccountTypesEnum type, String currency, String balance) {
		return new Account("a", "i", type, null, "n", null, balance == null ? null : new BigDecimal(balance),
				currency, null);
	}

	private static Transaction tx(TransactionTypesEnum type, String currency, String amount) {
		return new Transaction("t", null, "d", amount == null ? null : new BigDecimal(amount), currency, type, null,
				null);
	}

}
