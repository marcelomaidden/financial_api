package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Account;
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

	@Test
	void serializesIncomesPartialAndErrors() {
		AccountService accountService = mock(AccountService.class);
		TransactionService transactionService = mock(TransactionService.class);
		Account nubank = new Account("m1", "item", AccountTypesEnum.BANK, null, "Nubank", null,
				new BigDecimal("500.50"), "BRL", null);
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");
		when(accountService.getAccounts(null)).thenReturn(new OwnersResponse<>(List.of(
				new OwnerAccountsResult("marcelo", List.of(nubank), null),
				new OwnerAccountsResult("luana", List.of(), error))));
		when(transactionService.getAllTransactions("marcelo", "m1", LocalDate.of(2026, 11, 1), TODAY))
				.thenReturn(List.of(new Transaction("t1", null, "PIX Recebido", new BigDecimal("19.99"), "BRL",
						TransactionTypesEnum.CREDIT, "POSTED", null)));
		Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
		SummaryService service = new SummaryService(accountService, new IncomesSummaryService(transactionService),
				clock);

		JsonNode json = JsonMapper.builder().build().valueToTree(service.getSummary(null, null));

		assertThat(json.path("partial").asBoolean()).isTrue();
		assertThat(json.path("errors")).hasSize(1);
		assertThat(json.path("errors").get(0).path("owner").asString()).isEqualTo("luana");
		assertThat(json.path("errors").get(0).path("status").asInt()).isEqualTo(502);
		assertThat(json.path("errors").get(0).path("message").asString()).isEqualTo("Erro ao consultar a Pluggy");

		JsonNode available = json.path("incomes").path("available");
		assertThat(available.path("total").path("value").decimalValue()).isEqualByComparingTo("500.50");
		assertThat(available.path("total").path("currencyCode").asString()).isEqualTo("BRL");
		JsonNode account = available.path("detail").path("marcelo").get(0);
		assertThat(account.path("id").asString()).isEqualTo("m1");
		assertThat(account.path("name").asString()).isEqualTo("Nubank");
		assertThat(account.path("balance").decimalValue()).isEqualByComparingTo("500.50");
		assertThat(account.path("currencyCode").asString()).isEqualTo("BRL");

		JsonNode transactions = json.path("incomes").path("transactions");
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

}
