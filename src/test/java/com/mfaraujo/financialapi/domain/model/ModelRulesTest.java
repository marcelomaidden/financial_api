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

	@Test
	void brlDebitNeedsDebitTypeBrlAndAmount() {
		assertThat(tx(TransactionTypesEnum.DEBIT, "BRL", "5").isBrlDebit()).isTrue();
		assertThat(tx(TransactionTypesEnum.CREDIT, "BRL", "5").isBrlDebit()).isFalse();
		assertThat(tx(TransactionTypesEnum.UNKNOWN, "BRL", "5").isBrlDebit()).isFalse();
		assertThat(tx(TransactionTypesEnum.DEBIT, "USD", "5").isBrlDebit()).isFalse();
		assertThat(tx(TransactionTypesEnum.DEBIT, "BRL", null).isBrlDebit()).isFalse();
	}

	@Test
	void brlCreditCardNeedsCreditTypeAndBrl() {
		assertThat(account(AccountTypesEnum.CREDIT, "BRL", "1").isBrlCreditCard()).isTrue();
		assertThat(account(AccountTypesEnum.CREDIT, "USD", "1").isBrlCreditCard()).isFalse();
		assertThat(account(AccountTypesEnum.CREDIT, null, "1").isBrlCreditCard()).isFalse();
		assertThat(account(AccountTypesEnum.BANK, "BRL", "1").isBrlCreditCard()).isFalse();
	}

	@Test
	void unbilledPendingDebitNeedsDebitPendingAndNoBillId() {
		assertThat(purchase(TransactionTypesEnum.DEBIT, "PENDING", null).isUnbilledPendingDebit()).isTrue();
		assertThat(purchase(TransactionTypesEnum.DEBIT, "PENDING", "").isUnbilledPendingDebit()).isTrue();
		assertThat(purchase(TransactionTypesEnum.DEBIT, "PENDING", " ").isUnbilledPendingDebit()).isTrue();
		assertThat(purchase(TransactionTypesEnum.DEBIT, "PENDING", "bill-1").isUnbilledPendingDebit()).isFalse();
		assertThat(purchase(TransactionTypesEnum.DEBIT, "POSTED", null).isUnbilledPendingDebit()).isFalse();
		assertThat(purchase(TransactionTypesEnum.DEBIT, null, null).isUnbilledPendingDebit()).isFalse();
		assertThat(purchase(TransactionTypesEnum.CREDIT, "PENDING", null).isUnbilledPendingDebit()).isFalse();
		assertThat(purchase(TransactionTypesEnum.UNKNOWN, "PENDING", null).isUnbilledPendingDebit()).isFalse();
	}

	@Test
	void amountInBrlUsesAmountForBrlAndAccountCurrencyForOthers() {
		assertThat(foreign("BRL", "30", null).amountInBrl()).isEqualByComparingTo("30");
		assertThat(foreign("BRL", "30", "99").amountInBrl()).isEqualByComparingTo("30");
		assertThat(foreign("USD", "58", "298.19").amountInBrl()).isEqualByComparingTo("298.19");
		assertThat(foreign("USD", "58", null).amountInBrl()).isNull();
		assertThat(foreign("BRL", null, null).amountInBrl()).isNull();
	}

	@Test
	void brlBillNeedsBrlAndAmount() {
		assertThat(bill("BRL", "100").isBrl()).isTrue();
		assertThat(bill("USD", "100").isBrl()).isFalse();
		assertThat(bill(null, "100").isBrl()).isFalse();
		assertThat(bill("BRL", null).isBrl()).isFalse();
	}

	private static Transaction purchase(TransactionTypesEnum type, String status, String billId) {
		return new Transaction("t", null, "d", BigDecimal.TEN, "BRL", type, status, null, billId, null, null);
	}

	private static Transaction foreign(String currency, String amount, String inAccountCurrency) {
		return new Transaction("t", null, "d", amount == null ? null : new BigDecimal(amount), currency,
				TransactionTypesEnum.DEBIT, "PENDING", null, null,
				inAccountCurrency == null ? null : new BigDecimal(inAccountCurrency), null);
	}

	private static Bill bill(String currency, String total) {
		return new Bill("b", null, null, total == null ? null : new BigDecimal(total), currency, null);
	}

	private static Account account(AccountTypesEnum type, String currency, String balance) {
		return new Account("a", "i", type, null, "n", null, balance == null ? null : new BigDecimal(balance),
				currency, null);
	}

	private static Transaction tx(TransactionTypesEnum type, String currency, String amount) {
		return new Transaction("t", null, "d", amount == null ? null : new BigDecimal(amount), currency, type, null,
				null, null, null, null);
	}

}
