package com.mfaraujo.financialapi.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.BillDetail;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.BillDetailResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Fixa os nomes de campo do JSON de {@code GET /bill/{id}}. */
class BillDetailJsonTest {

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	void serializesTheBillAndItsPurchases() {
		BillDetail bill = new BillDetail("b1", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10),
				new BigDecimal("1500.75"), "BRL", new BigDecimal("225"), true,
				List.of(new BillDetail.FinanceCharge("fc1", "IOF", new BigDecimal("12.5"), "BRL", "info")),
				List.of(new BillDetail.Payment("p1", "TOTAL", LocalDate.of(2026, 11, 18), "PIX", new BigDecimal("300"),
						"BRL")));
		Transaction installment = new Transaction("t1", OffsetDateTime.parse("2026-11-10T12:00:00Z"), "Loja",
				new BigDecimal("50"), "BRL", TransactionTypesEnum.DEBIT, "POSTED", "Compras", "b1", null,
				new Transaction.Installment(3, 10));

		JsonNode json = mapper.valueToTree(ResponseMapper.toBillDetail("marcelo", "c1",
				new BillDetailResult(bill, List.of(installment))));

		assertThat(json.path("owner").asString()).isEqualTo("marcelo");
		assertThat(json.path("accountId").asString()).isEqualTo("c1");
		JsonNode b = json.path("bill");
		assertThat(b.path("id").asString()).isEqualTo("b1");
		assertThat(b.path("dueDate").asString()).isEqualTo("2026-11-20");
		assertThat(b.path("closingDate").asString()).isEqualTo("2026-11-10");
		assertThat(b.path("totalAmount").decimalValue()).isEqualByComparingTo("1500.75");
		assertThat(b.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(b.path("minimumPaymentAmount").decimalValue()).isEqualByComparingTo("225");
		assertThat(b.path("allowsInstallments").asBoolean()).isTrue();
		JsonNode charge = b.path("financeCharges").get(0);
		assertThat(charge.path("id").asString()).isEqualTo("fc1");
		assertThat(charge.path("type").asString()).isEqualTo("IOF");
		assertThat(charge.path("amount").decimalValue()).isEqualByComparingTo("12.5");
		assertThat(charge.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(charge.path("additionalInfo").asString()).isEqualTo("info");
		JsonNode payment = b.path("payments").get(0);
		assertThat(payment.path("id").asString()).isEqualTo("p1");
		assertThat(payment.path("valueType").asString()).isEqualTo("TOTAL");
		assertThat(payment.path("paymentDate").asString()).isEqualTo("2026-11-18");
		assertThat(payment.path("paymentMode").asString()).isEqualTo("PIX");
		assertThat(payment.path("amount").decimalValue()).isEqualByComparingTo("300");
		assertThat(payment.path("currencyCode").asString()).isEqualTo("BRL");
		JsonNode tx = json.path("transactions").get(0);
		assertThat(tx.path("id").asString()).isEqualTo("t1");
		assertThat(tx.path("date").asString()).isEqualTo("2026-11-10");
		assertThat(tx.path("description").asString()).isEqualTo("Loja");
		assertThat(tx.path("amount").decimalValue()).isEqualByComparingTo("50");
		assertThat(tx.path("currencyCode").asString()).isEqualTo("BRL");
		assertThat(tx.path("category").asString()).isEqualTo("Compras");
		assertThat(tx.path("type").asString()).isEqualTo("DEBIT");
		assertThat(tx.path("status").asString()).isEqualTo("POSTED");
		assertThat(tx.path("installment").path("number").asInt()).isEqualTo(3);
		assertThat(tx.path("installment").path("total").asInt()).isEqualTo(10);
	}

	@Test
	void omitsInstallmentOfASinglePurchaseAndKeepsOtherNullsAndEmptyLists() {
		BillDetail bill = new BillDetail("b1", LocalDate.of(2026, 11, 20), null, new BigDecimal("10"), "BRL", null,
				null, List.of(), List.of());
		Transaction single = new Transaction("t2", OffsetDateTime.parse("2026-11-10T12:00:00Z"), "Café",
				new BigDecimal("5"), "BRL", TransactionTypesEnum.DEBIT, "POSTED", null, "b1", null, null);

		JsonNode json = mapper.valueToTree(ResponseMapper.toBillDetail("marcelo", "c1",
				new BillDetailResult(bill, List.of(single))));

		JsonNode b = json.path("bill");
		assertThat(b.has("closingDate")).isTrue();
		assertThat(b.path("closingDate").isNull()).isTrue();
		assertThat(b.path("minimumPaymentAmount").isNull()).isTrue();
		assertThat(b.path("allowsInstallments").isNull()).isTrue();
		assertThat(b.path("financeCharges").isArray()).isTrue();
		assertThat(b.path("financeCharges")).isEmpty();
		assertThat(b.path("payments")).isEmpty();
		JsonNode tx = json.path("transactions").get(0);
		assertThat(tx.has("installment")).isFalse();
		assertThat(tx.path("category").isNull()).isTrue();
	}

	@Test
	void serializesTheAmountInAccountCurrencyOnlyForPurchasesOutsideBrlThatHaveIt() {
		BillDetail bill = new BillDetail("b1", LocalDate.of(2026, 11, 20), null, new BigDecimal("10"), "BRL", null,
				null, List.of(), List.of());
		OffsetDateTime date = OffsetDateTime.parse("2026-11-10T12:00:00Z");
		Transaction usd = new Transaction("usd", date, "Netflix", new BigDecimal("-58"), "USD",
				TransactionTypesEnum.DEBIT, "POSTED", null, "b1", new BigDecimal("-298.19"), null);
		Transaction brl = new Transaction("brl", date, "Café", new BigDecimal("-5"), "BRL", TransactionTypesEnum.DEBIT,
				"POSTED", null, "b1", new BigDecimal("-5"), null);
		Transaction usdWithoutValue = new Transaction("usd2", date, "Loja", new BigDecimal("-10"), "USD",
				TransactionTypesEnum.DEBIT, "POSTED", null, "b1", null, null);

		JsonNode json = mapper.valueToTree(ResponseMapper.toBillDetail("marcelo", "c1",
				new BillDetailResult(bill, List.of(usd, brl, usdWithoutValue))));

		JsonNode transactions = json.path("transactions");
		assertThat(transactions.get(0).path("amountInAccountCurrency").isNumber()).isTrue();
		assertThat(transactions.get(0).path("amountInAccountCurrency").decimalValue()).isEqualByComparingTo("-298.19");
		assertThat(transactions.get(0).path("amount").decimalValue()).isEqualByComparingTo("-58");
		assertThat(transactions.get(0).path("currencyCode").asString()).isEqualTo("USD");
		assertThat(transactions.get(1).has("amountInAccountCurrency")).isFalse();
		assertThat(transactions.get(2).has("amountInAccountCurrency")).isFalse();
	}

}
