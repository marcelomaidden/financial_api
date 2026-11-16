package com.mfaraujo.financialapi.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.AccountFailure;
import com.mfaraujo.financialapi.domain.result.OwnedTransaction;
import com.mfaraujo.financialapi.domain.result.TransactionListResult;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fixa os nomes de campo do JSON de {@code GET /transactions}. */
class ConsolidatedTransactionsJsonTest {

	@Test
	void serializesTransactionsPartialAndErrors() {
		Transaction tx = new Transaction("t1", OffsetDateTime.parse("2026-11-10T00:00:00Z"), "PIX Recebido",
				new BigDecimal("19.99"), "BRL", TransactionTypesEnum.CREDIT, "POSTED", "Transfer", null, null, null);
		TransactionListResult result = new TransactionListResult(List.of(new OwnedTransaction("marcelo", "a1", tx)),
				List.of(new AccountFailure("marcelo", "a2", new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy")),
						new AccountFailure("luana", null, new ApiError(429, "Too Many Requests", "Limite atingido"))));

		JsonNode json = JsonMapper.builder().build().valueToTree(ResponseMapper.toConsolidatedTransactions(result));

		assertThat(json.path("partial").asBoolean()).isTrue();
		JsonNode item = json.path("transactions").get(0);
		assertThat(item.path("owner").asString()).isEqualTo("marcelo");
		assertThat(item.path("accountId").asString()).isEqualTo("a1");
		assertThat(item.path("id").asString()).isEqualTo("t1");
		assertThat(item.has("date")).isTrue();
		assertThat(item.path("description").asString()).isEqualTo("PIX Recebido");
		assertThat(item.path("amount").decimalValue()).isEqualByComparingTo("19.99");
		assertThat(item.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(item.path("type").asString()).isEqualTo("CREDIT");
		assertThat(item.path("status").asString()).isEqualTo("POSTED");
		assertThat(item.path("category").asString()).isEqualTo("Transfer");

		JsonNode accountError = json.path("errors").get(0);
		assertThat(accountError.path("owner").asString()).isEqualTo("marcelo");
		assertThat(accountError.path("accountId").asString()).isEqualTo("a2");
		assertThat(accountError.path("status").asInt()).isEqualTo(502);
		assertThat(accountError.path("message").asString()).isEqualTo("Erro ao consultar a Pluggy");
		JsonNode ownerError = json.path("errors").get(1);
		assertThat(ownerError.path("owner").asString()).isEqualTo("luana");
		assertThat(ownerError.has("accountId")).isTrue();
		assertThat(ownerError.path("accountId").isNull()).isTrue();
	}

	@Test
	void serializesAnEmptyResultAsNotPartial() {
		JsonNode json = JsonMapper.builder().build().valueToTree(
				ResponseMapper.toConsolidatedTransactions(new TransactionListResult(List.of(), List.of())));

		assertThat(json.path("transactions")).isEmpty();
		assertThat(json.path("partial").asBoolean()).isFalse();
		assertThat(json.path("errors")).isEmpty();
	}

}
