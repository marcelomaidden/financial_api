package com.mfaraujo.financialapi.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.result.BillPage;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.OwnerCreditCardsResult;
import com.mfaraujo.financialapi.domain.result.TransactionPage;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.BillResponse;
import com.mfaraujo.financialapi.resource.dto.BillsResponse;
import com.mfaraujo.financialapi.resource.dto.CreditCardResponse;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;
import org.junit.jupiter.api.Test;

class ResponseMapperTest {

	private static final OffsetDateTime DATE = OffsetDateTime.parse("2026-11-10T12:00:00Z");

	@Test
	void mapsAccountWithTypeAsText() {
		Account account = new Account("a1", "item1", AccountTypesEnum.BANK, "CHECKING_ACCOUNT", "Nubank", "123",
				new BigDecimal("500"), "BRL", null);

		assertThat(ResponseMapper.toAccount(account)).isEqualTo(new AccountResponse("a1", "item1", "BANK",
				"CHECKING_ACCOUNT", "Nubank", "123", new BigDecimal("500"), "BRL"));
	}

	@Test
	void unknownTypesAreExposedAsUnknown() {
		Account account = new Account("a1", "i", AccountTypesEnum.UNKNOWN, null, "x", null, BigDecimal.ONE, "BRL",
				null);
		Transaction tx = new Transaction("t", DATE, "x", BigDecimal.ONE, "BRL", TransactionTypesEnum.UNKNOWN, null,
				null, null, null);

		assertThat(ResponseMapper.toAccount(account).type()).isEqualTo("UNKNOWN");
		assertThat(ResponseMapper.toTransaction(tx).type()).isEqualTo("UNKNOWN");
	}

	@Test
	void mapsCreditCardWithAndWithoutCreditData() {
		Account.Credit credit = new Account.Credit("VISA", new BigDecimal("1000"), new BigDecimal("800"),
				LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
		Account withCredit = new Account("c1", "item1", AccountTypesEnum.CREDIT, null, "Cartão", "9999",
				new BigDecimal("200"), "BRL", credit);
		Account withoutCredit = new Account("c2", "item1", AccountTypesEnum.CREDIT, null, "Cartão 2", "8888",
				new BigDecimal("10"), "BRL", null);

		assertThat(ResponseMapper.toCreditCard(withCredit)).isEqualTo(new CreditCardResponse("c1", "item1", "Cartão",
				"9999", "VISA", new BigDecimal("200"), "BRL", new BigDecimal("1000"), new BigDecimal("800"),
				LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)));
		assertThat(ResponseMapper.toCreditCard(withoutCredit)).isEqualTo(new CreditCardResponse("c2", "item1",
				"Cartão 2", "8888", null, new BigDecimal("10"), "BRL", null, null, null, null));
	}

	@Test
	void mapsTransactionsPage() {
		Transaction tx = new Transaction("t1", DATE, "PIX", new BigDecimal("19.99"), "BRL",
				TransactionTypesEnum.CREDIT, "POSTED", "Transfer", null, null);

		TransactionsResponse response = ResponseMapper.toTransactions("marcelo", "a1",
				new TransactionPage(List.of(tx), "cursor"));

		assertThat(response.owner()).isEqualTo("marcelo");
		assertThat(response.accountId()).isEqualTo("a1");
		assertThat(response.nextCursor()).isEqualTo("cursor");
		assertThat(response.transactions()).containsExactly(new TransactionResponse("t1", DATE, "PIX",
				new BigDecimal("19.99"), "BRL", "CREDIT", "POSTED", "Transfer"));
	}

	@Test
	void mapsBillsPage() {
		LocalDateTime due = LocalDateTime.of(2026, 11, 12, 0, 0);
		LocalDateTime closing = LocalDateTime.of(2026, 11, 5, 0, 0);
		Bill bill = new Bill("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30"));

		BillsResponse response = ResponseMapper.toBills("luana", "c1", new BillPage(List.of(bill), 1, 3));

		assertThat(response.owner()).isEqualTo("luana");
		assertThat(response.accountId()).isEqualTo("c1");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.bills()).containsExactly(
				new BillResponse("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30")));
	}

	@Test
	void keepsOwnerErrorInAccountsAndCreditCards() {
		ApiError error = new ApiError(502, "Bad Gateway", "falhou");
		Account account = new Account("a1", "i", AccountTypesEnum.BANK, null, "x", null, BigDecimal.ONE, "BRL", null);

		OwnersResponse<OwnerAccounts> accounts = ResponseMapper.toAccounts(new OwnersResponse<>(List.of(
				new OwnerAccountsResult("marcelo", List.of(account), null),
				new OwnerAccountsResult("luana", List.of(), error))));
		OwnersResponse<OwnerCreditCards> cards = ResponseMapper.toCreditCards(new OwnersResponse<>(List.of(
				new OwnerCreditCardsResult("luana", List.of(), error))));

		assertThat(accounts.owners()).extracting(OwnerAccounts::owner).containsExactly("marcelo", "luana");
		assertThat(accounts.owners().get(0).accounts()).hasSize(1);
		assertThat(accounts.owners().get(0).error()).isNull();
		assertThat(accounts.owners().get(1).error()).isEqualTo(error);
		assertThat(cards.owners().get(0).creditCards()).isEmpty();
		assertThat(cards.owners().get(0).error()).isEqualTo(error);
	}

}
