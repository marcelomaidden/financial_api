package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import org.junit.jupiter.api.Test;

class PluggyMapperTest {

	@Test
	void mapsBankAccountWithoutCredit() {
		PluggyAccount source = new PluggyAccount("a1", "item1", "BANK", "CHECKING_ACCOUNT", "123", "Nubank", null,
				new BigDecimal("500.50"), "BRL", null);

		Account account = PluggyMapper.toAccount(source);

		assertThat(account.id()).isEqualTo("a1");
		assertThat(account.itemId()).isEqualTo("item1");
		assertThat(account.type()).isEqualTo(AccountTypesEnum.BANK);
		assertThat(account.subtype()).isEqualTo("CHECKING_ACCOUNT");
		assertThat(account.name()).isEqualTo("Nubank");
		assertThat(account.number()).isEqualTo("123");
		assertThat(account.balance()).isEqualByComparingTo("500.50");
		assertThat(account.currencyCode()).isEqualTo("BRL");
		assertThat(account.credit()).isNull();
	}

	@Test
	void mapsCreditCardWithCreditData() {
		PluggyAccount.CreditData credit = new PluggyAccount.CreditData("GOLD", "VISA", LocalDate.of(2026, 11, 5),
				LocalDate.of(2026, 11, 12), new BigDecimal("800"), new BigDecimal("1000"), new BigDecimal("50"),
				"ACTIVE");
		PluggyAccount source = new PluggyAccount("c1", "item1", "CREDIT", "CREDIT_CARD", "9999", "Cartão", null,
				new BigDecimal("200"), "BRL", credit);

		Account account = PluggyMapper.toAccount(source);

		assertThat(account.isCreditCard()).isTrue();
		assertThat(account.credit()).isEqualTo(new Account.Credit("VISA", new BigDecimal("1000"),
				new BigDecimal("800"), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)));
	}

	@Test
	void unknownAccountTypeBecomesUnknown() {
		PluggyAccount source = new PluggyAccount("a1", "i", "INVESTMENT", null, null, "x", null, BigDecimal.ONE,
				"BRL", null);

		assertThat(PluggyMapper.toAccount(source).type()).isEqualTo(AccountTypesEnum.UNKNOWN);
	}

	@Test
	void mapsTransactionAndUnknownType() {
		OffsetDateTime date = OffsetDateTime.parse("2026-11-10T12:00:00Z");
		PluggyTransaction source = new PluggyTransaction("t1", "a1", date, "PIX Recebido", new BigDecimal("19.99"),
				"BRL", "CREDIT", "POSTED", "Transfer", null);

		Transaction tx = PluggyMapper.toTransaction(source);

		assertThat(tx).isEqualTo(new Transaction("t1", date, "PIX Recebido", new BigDecimal("19.99"), "BRL",
				TransactionTypesEnum.CREDIT, "POSTED", "Transfer"));
		assertThat(PluggyMapper.toTransaction(new PluggyTransaction("t2", "a1", date, "x", BigDecimal.ONE, "BRL",
				"REFUND", null, null, null)).type()).isEqualTo(TransactionTypesEnum.UNKNOWN);
	}

	@Test
	void mapsBill() {
		LocalDateTime due = LocalDateTime.of(2026, 11, 12, 0, 0);
		LocalDateTime closing = LocalDateTime.of(2026, 11, 5, 0, 0);
		PluggyBill source = new PluggyBill("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30"));

		assertThat(PluggyMapper.toBill(source)).isEqualTo(
				new Bill("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30")));
	}

}
