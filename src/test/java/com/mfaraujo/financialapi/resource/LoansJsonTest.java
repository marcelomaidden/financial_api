package com.mfaraujo.financialapi.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.LoanKindEnum;
import com.mfaraujo.financialapi.domain.model.Loan;
import com.mfaraujo.financialapi.domain.result.OwnerLoansResult;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fixa os nomes de campo do JSON de {@code GET /loans}. */
class LoansJsonTest {

	@Test
	void serializesLoansAndOwnerErrors() {
		Loan loan = new Loan("l1", "item1", "0007", "Crédito Consignado", LoanKindEnum.LOAN,
				"CREDITO_PESSOAL_COM_CONSIGNACAO", new BigDecimal("50000"), "BRL", LocalDate.of(2022, 8, 1),
				LocalDate.of(2028, 1, 15), new Loan.Installments(120, 65, 55, 0), new BigDecimal("25000"),
				new Loan.Payment(LocalDate.of(2023, 7, 15), new BigDecimal("680.50")));
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");

		JsonNode json = JsonMapper.builder().build().valueToTree(ResponseMapper.toLoans(new OwnersResponse<>(List.of(
				new OwnerLoansResult("marcelo", List.of(loan), null),
				new OwnerLoansResult("luana", List.of(), error)))));

		JsonNode marcelo = json.path("owners").get(0);
		assertThat(marcelo.path("owner").asString()).isEqualTo("marcelo");
		assertThat(marcelo.path("error").isNull()).isTrue();
		JsonNode response = marcelo.path("loans").get(0);
		assertThat(response.path("id").asString()).isEqualTo("l1");
		assertThat(response.path("itemId").asString()).isEqualTo("item1");
		assertThat(response.path("contractNumber").asString()).isEqualTo("0007");
		assertThat(response.path("productName").asString()).isEqualTo("Crédito Consignado");
		assertThat(response.path("kind").asString()).isEqualTo("LOAN");
		assertThat(response.path("type").asString()).isEqualTo("CREDITO_PESSOAL_COM_CONSIGNACAO");
		assertThat(response.path("contractAmount").decimalValue()).isEqualByComparingTo("50000");
		assertThat(response.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(response.path("contractDate").asString()).isEqualTo("2022-08-01");
		assertThat(response.path("dueDate").asString()).isEqualTo("2028-01-15");
		assertThat(response.path("installments").path("total").asInt()).isEqualTo(120);
		assertThat(response.path("installments").path("paid").asInt()).isEqualTo(65);
		assertThat(response.path("installments").path("remaining").asInt()).isEqualTo(55);
		assertThat(response.path("installments").path("pastDue").asInt()).isZero();
		assertThat(response.path("outstandingBalance").decimalValue()).isEqualByComparingTo("25000");
		assertThat(response.path("lastPayment").path("paidDate").asString()).isEqualTo("2023-07-15");
		assertThat(response.path("lastPayment").path("paidAmount").decimalValue()).isEqualByComparingTo("680.50");

		JsonNode luana = json.path("owners").get(1);
		assertThat(luana.path("loans")).isEmpty();
		assertThat(luana.path("error").path("status").asInt()).isEqualTo(502);
		assertThat(luana.path("error").path("message").asString()).isEqualTo("Erro ao consultar a Pluggy");
	}

	@Test
	void serializesMissingFieldsAsNull() {
		Loan loan = new Loan("l2", "item1", null, "Cheque especial", LoanKindEnum.UNARRANGED_ACCOUNT_OVERDRAFT, null,
				null, "BRL", null, null, null, null, null);

		JsonNode response = JsonMapper.builder().build().valueToTree(ResponseMapper.toLoans(new OwnersResponse<>(
				List.of(new OwnerLoansResult("marcelo", List.of(loan), null))))).path("owners").get(0).path("loans")
				.get(0);

		assertThat(response.path("kind").asString()).isEqualTo("UNARRANGED_ACCOUNT_OVERDRAFT");
		assertThat(response.has("installments")).isTrue();
		assertThat(response.path("installments").isNull()).isTrue();
		assertThat(response.path("lastPayment").isNull()).isTrue();
		assertThat(response.path("outstandingBalance").isNull()).isTrue();
		assertThat(response.path("contractAmount").isNull()).isTrue();
	}

}
