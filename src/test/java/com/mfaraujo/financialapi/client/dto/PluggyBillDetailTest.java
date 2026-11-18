package com.mfaraujo.financialapi.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Lê o JSON de exemplo da documentação da Pluggy, com campos que o sistema ignora. */
class PluggyBillDetailTest {

	private static final String DOC_EXAMPLE = """
			{
			  "id": "b1f0a5b4-1234-5678-9abc-def012345678",
			  "dueDate": "2023-07-20T00:00:00",
			  "billClosingDate": "2023-07-10T00:00:00.000Z",
			  "totalAmount": 1520.75,
			  "totalAmountCurrencyCode": "BRL",
			  "minimumPaymentAmount": 228.11,
			  "allowsInstallments": true,
			  "createdAt": "2023-07-01T00:00:00.000Z",
			  "financeCharges": [{
			    "id": "fc1",
			    "type": "IOF",
			    "amount": 12.5,
			    "currencyCode": "BRL",
			    "additionalInfo": "IOF internacional",
			    "unknown": 1
			  }],
			  "payments": [{
			    "id": "p1",
			    "valueType": "TOTAL",
			    "paymentDate": "2023-07-18T00:00:00.000Z",
			    "paymentMode": "PIX",
			    "amount": 300,
			    "currencyCode": "BRL"
			  }]
			}
			""";

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	void readsTheDocumentationExample() {
		PluggyBillDetail bill = mapper.readValue(DOC_EXAMPLE, PluggyBillDetail.class);

		assertThat(bill.id()).isEqualTo("b1f0a5b4-1234-5678-9abc-def012345678");
		assertThat(bill.totalAmount()).isEqualByComparingTo("1520.75");
		assertThat(bill.totalAmountCurrencyCode()).isEqualTo("BRL");
		assertThat(bill.minimumPaymentAmount()).isEqualByComparingTo("228.11");
		assertThat(bill.allowsInstallments()).isTrue();
		assertThat(bill.financeChargesOrEmpty()).hasSize(1);
		PluggyBillDetail.FinanceCharge charge = bill.financeChargesOrEmpty().get(0);
		assertThat(charge.id()).isEqualTo("fc1");
		assertThat(charge.type()).isEqualTo("IOF");
		assertThat(charge.amount()).isEqualByComparingTo("12.5");
		assertThat(charge.currencyCode()).isEqualTo("BRL");
		assertThat(charge.additionalInfo()).isEqualTo("IOF internacional");
		assertThat(bill.paymentsOrEmpty()).hasSize(1);
		PluggyBillDetail.Payment payment = bill.paymentsOrEmpty().get(0);
		assertThat(payment.id()).isEqualTo("p1");
		assertThat(payment.valueType()).isEqualTo("TOTAL");
		assertThat(payment.paymentMode()).isEqualTo("PIX");
		assertThat(payment.amount()).isEqualByComparingTo("300");
		assertThat(payment.currencyCode()).isEqualTo("BRL");
	}

	@Test
	void readsDatesWithAndWithoutZKeepingTheDay() {
		PluggyBillDetail bill = mapper.readValue(DOC_EXAMPLE, PluggyBillDetail.class);

		assertThat(bill.dueDate().toLocalDate()).isEqualTo(LocalDate.of(2023, 7, 20));
		assertThat(bill.billClosingDate().toLocalDate()).isEqualTo(LocalDate.of(2023, 7, 10));
		assertThat(bill.paymentsOrEmpty().get(0).paymentDate().toLocalDate()).isEqualTo(LocalDate.of(2023, 7, 18));
	}

	@Test
	void toleratesMissingOptionalFieldsAndLists() {
		PluggyBillDetail bill = mapper.readValue("""
				{ "id": "b1", "dueDate": "2023-07-20T00:00:00", "totalAmount": 10,
				  "totalAmountCurrencyCode": "BRL" }
				""", PluggyBillDetail.class);

		assertThat(bill.billClosingDate()).isNull();
		assertThat(bill.minimumPaymentAmount()).isNull();
		assertThat(bill.allowsInstallments()).isNull();
		assertThat(bill.financeCharges()).isNull();
		assertThat(bill.payments()).isNull();
		assertThat(bill.financeChargesOrEmpty()).isEmpty();
		assertThat(bill.paymentsOrEmpty()).isEmpty();
	}

}
