package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fixa os nomes de campo do JSON de {@code GET /summary}. */
class SummaryJsonTest {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

	private static final OffsetDateTime DATE = OffsetDateTime.parse("2026-11-10T12:00:00Z");

	@Test
	void serializesIncomesExpensesPartialAndErrors() {
		JsonNode json = summaryJson();

		assertThat(json.path("partial").asBoolean()).isTrue();
		assertThat(json.path("errors")).hasSize(1);
		assertThat(json.path("errors").get(0).path("owner").asString()).isEqualTo("luana");
		assertThat(json.path("errors").get(0).path("status").asInt()).isEqualTo(502);
		assertThat(json.path("errors").get(0).path("message").asString()).isEqualTo("Erro ao consultar a Pluggy");
	}

	@Test
	void serializesIncomes() {
		JsonNode incomes = summaryJson().path("incomes");

		JsonNode available = incomes.path("available");
		assertThat(available.path("total").path("value").decimalValue()).isEqualByComparingTo("500.50");
		assertThat(available.path("total").path("currencyCode").asString()).isEqualTo("BRL");
		JsonNode account = available.path("detail").path("marcelo").get(0);
		assertThat(account.path("id").asString()).isEqualTo("m1");
		assertThat(account.path("name").asString()).isEqualTo("Nubank");
		assertThat(account.path("balance").decimalValue()).isEqualByComparingTo("500.50");
		assertThat(account.path("currencyCode").asString()).isEqualTo("BRL");

		JsonNode transactions = incomes.path("transactions");
		assertThat(transactions.path("total").path("value").decimalValue()).isEqualByComparingTo("19.99");
		JsonNode tx = transactions.path("detail").path("marcelo").get(0);
		assertThat(tx.path("id").asString()).isEqualTo("t1");
		assertThat(tx.path("description").asString()).isEqualTo("PIX Recebido");
		assertThat(tx.path("amount").decimalValue()).isEqualByComparingTo("19.99");
		assertThat(tx.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(tx.path("accountId").asString()).isEqualTo("m1");
		assertThat(tx.has("date")).isTrue();
		assertThat(available.path("detail").has("luana")).isFalse();
		assertThat(transactions.path("detail").has("luana")).isFalse();
	}

	@Test
	void serializesExpensesAccountDebitsWithPositiveAmounts() {
		JsonNode debits = summaryJson().path("expenses").path("accountDebits");

		assertThat(debits.path("total").path("value").decimalValue()).isEqualByComparingTo("42.50");
		assertThat(debits.path("total").path("currencyCode").asString()).isEqualTo("BRL");
		JsonNode tx = debits.path("detail").path("marcelo").get(0);
		assertThat(tx.path("id").asString()).isEqualTo("d1");
		assertThat(tx.path("description").asString()).isEqualTo("Mercado");
		assertThat(tx.path("amount").decimalValue()).isEqualByComparingTo("42.50");
		assertThat(tx.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(tx.path("accountId").asString()).isEqualTo("m1");
		assertThat(tx.has("date")).isTrue();
		assertThat(debits.path("detail").has("luana")).isFalse();
	}

	@Test
	void serializesExpensesOwed() {
		JsonNode owed = summaryJson().path("expenses").path("owed");

		assertThat(owed.path("total").path("value").decimalValue()).isEqualByComparingTo("250.40");
		assertThat(owed.path("total").path("currencyCode").asString()).isEqualTo("BRL");
		JsonNode bill = owed.path("detail").path("marcelo").get(0);
		assertThat(bill.path("id").asString()).isEqualTo("b1");
		assertThat(bill.path("accountId").asString()).isEqualTo("c1");
		assertThat(bill.path("name").asString()).isEqualTo("Cartão Nubank");
		assertThat(bill.has("dueDate")).isTrue();
		assertThat(bill.path("totalAmount").decimalValue()).isEqualByComparingTo("250.40");
		assertThat(bill.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(owed.path("detail").has("luana")).isFalse();
	}

	@Test
	void serializesExpensesTransactionsWithAmountInAccountCurrencyOnlyForForeignPurchases() {
		JsonNode transactions = summaryJson().path("expenses").path("transactions");

		assertThat(transactions.path("total").path("value").decimalValue()).isEqualByComparingTo("478.19");
		assertThat(transactions.path("total").path("currencyCode").asString()).isEqualTo("BRL");
		JsonNode usd = transactions.path("detail").path("marcelo").get(0);
		assertThat(usd.path("id").asString()).isEqualTo("p1");
		assertThat(usd.path("accountId").asString()).isEqualTo("c1");
		assertThat(usd.path("description").asString()).isEqualTo("Netflix");
		assertThat(usd.path("currencyCode").asString()).isEqualTo("USD");
		assertThat(usd.path("amount").decimalValue()).isEqualByComparingTo("58");
		assertThat(usd.path("amountInAccountCurrency").decimalValue()).isEqualByComparingTo("298.19");
		assertThat(usd.has("date")).isTrue();
		assertThat(usd.path("category").asString()).isEqualTo("Video streaming");
		JsonNode brl = transactions.path("detail").path("marcelo").get(1);
		assertThat(brl.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(brl.has("amountInAccountCurrency")).isFalse();
		assertThat(transactions.path("detail").has("luana")).isFalse();
	}

	@Test
	void serializesInstallmentOnlyForInstallmentPurchases() {
		JsonNode detail = summaryJson().path("expenses").path("transactions").path("detail").path("marcelo");

		JsonNode installment = detail.get(2);
		assertThat(installment.path("id").asString()).isEqualTo("p3");
		assertThat(installment.path("installment").path("number").asInt()).isEqualTo(3);
		assertThat(installment.path("installment").path("total").asInt()).isEqualTo(10);
		assertThat(detail.get(0).has("installment")).isFalse();
		assertThat(detail.get(1).has("installment")).isFalse();
	}

	private JsonNode summaryJson() {
		AccountService accountService = mock(AccountService.class);
		TransactionService transactionService = mock(TransactionService.class);
		BillService billService = mock(BillService.class);
		Account nubank = new Account("m1", "item", AccountTypesEnum.BANK, null, "Nubank", null,
				new BigDecimal("500.50"), "BRL", null);
		Account card = new Account("c1", "item", AccountTypesEnum.CREDIT, null, "Cartão Nubank", null,
				new BigDecimal("320"), "BRL", null);
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");
		when(accountService.getAccounts(null)).thenReturn(new OwnersResponse<>(List.of(
				new OwnerAccountsResult("marcelo", List.of(nubank, card), null),
				new OwnerAccountsResult("luana", List.of(), error))));
		when(transactionService.getAllTransactions(anyString(), anyString(), any(),
				any())).thenReturn(List.of());
		when(transactionService.getAllTransactions("marcelo", "m1", LocalDate.of(2026, 11, 1), TODAY))
				.thenReturn(List.of(new Transaction("t1", DATE, "PIX Recebido", new BigDecimal("19.99"), "BRL",
						TransactionTypesEnum.CREDIT, "POSTED", null, null, null, null),
						new Transaction("d1", DATE, "Mercado", new BigDecimal("-42.50"), "BRL",
								TransactionTypesEnum.DEBIT, "POSTED", null, null, null, null)));
		when(transactionService.getAllCreatedSince(anyString(), anyString(), any())).thenReturn(List.of());
		when(transactionService.getAllCreatedSince("marcelo", "c1", Instant.parse("2026-11-03T03:00:00Z")))
				.thenReturn(List.of(
						new Transaction("p1", DATE, "Netflix", new BigDecimal("58"), "USD",
								TransactionTypesEnum.DEBIT, "PENDING", "Video streaming", null,
								new BigDecimal("298.19"), null),
						new Transaction("p2", DATE, "Mercado", new BigDecimal("30"), "BRL",
								TransactionTypesEnum.DEBIT, "PENDING", "Groceries", null, null, null),
						new Transaction("p3", OffsetDateTime.parse("2026-08-15T00:00:00Z"), "Notebook",
								new BigDecimal("150"), "BRL", TransactionTypesEnum.DEBIT, "PENDING", "Electronics",
								null, null, new Transaction.Installment(3, 10))));
		when(billService.getAllBills("marcelo", "c1")).thenReturn(List.of(new Bill("b1",
				LocalDateTime.of(2026, 11, 20, 0, 0), LocalDateTime.of(2026, 11, 13, 0, 0),
				new BigDecimal("250.40"), "BRL", new BigDecimal("25"))));
		Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
		SummaryService service = new SummaryService(accountService, new IncomesSummaryService(transactionService),
				new ExpensesSummaryService(billService, transactionService), clock);

		return JsonMapper.builder().build().valueToTree(service.getSummary(null, null));
	}

}
