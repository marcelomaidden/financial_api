package com.mfaraujo.financialapi.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Lê o JSON de exemplo da documentação da Pluggy, com campos que o sistema ignora. */
class PluggyLoanTest {

	private static final String DOC_EXAMPLE = """
			{
			  "page": 1, "total": 1, "totalPages": 1,
			  "results": [{
			    "id": "658f07f1-8349-44cc-9590-e10fe9337060",
			    "itemId": "a9e42ebd-bddc-4d59-8895-140d8a809799",
			    "contractNumber": "000000721792794",
			    "ipocCode": "92792126019929279212650822221989319252576",
			    "productName": "Credito Pessoal Consignado",
			    "type": "CREDITO_PESSOAL_COM_CONSIGNACAO",
			    "kind": "LOAN",
			    "date": "2023-07-20T00:00:00.000Z",
			    "contractDate": "2022-08-01T00:00:00.000Z",
			    "disbursementDates": ["2018-01-15T00:00:00.000Z"],
			    "contractAmount": 50000,
			    "currencyCode": "BRL",
			    "dueDate": "2028-01-15T00:00:00.000Z",
			    "installmentPeriodicity": "MONTHLY",
			    "CET": 0.29,
			    "interestRates": [{ "taxType": "EFFECTIVE", "preFixedRate": 0.6 }],
			    "warranties": [{ "currencyCode": "BRL", "amount": 500 }],
			    "installments": {
			      "typeNumberOfInstallments": "MONTH",
			      "totalNumberOfInstallments": 120,
			      "contractRemainingNumber": 55,
			      "paidInstallments": 65,
			      "dueInstallments": 55,
			      "pastDueInstallments": 0,
			      "balloonPayments": []
			    },
			    "payments": {
			      "contractOutstandingBalance": 25000,
			      "releases": [{
			        "isOverParcelPayment": false,
			        "installmentId": "b1f0a5b4-1234-5678-9abc-def012345678",
			        "paidDate": "2023-07-15T00:00:00.000Z",
			        "currencyCode": "BRL",
			        "paidAmount": 680.5,
			        "overParcel": { "fees": [], "charges": [] }
			      }, {
			        "isOverParcelPayment": true,
			        "paidDate": "2023-08-01T00:00:00.000Z",
			        "paidAmount": 1000
			      }]
			    }
			  }]
			}
			""";

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	void readsTheDocumentationExample() {
		PluggyPage<PluggyLoan> page = mapper.readValue(DOC_EXAMPLE, new TypeReference<>() {
		});

		assertThat(page.resultsOrEmpty()).hasSize(1);
		PluggyLoan loan = page.resultsOrEmpty().get(0);
		assertThat(loan.id()).isEqualTo("658f07f1-8349-44cc-9590-e10fe9337060");
		assertThat(loan.itemId()).isEqualTo("a9e42ebd-bddc-4d59-8895-140d8a809799");
		assertThat(loan.contractNumber()).isEqualTo("000000721792794");
		assertThat(loan.productName()).isEqualTo("Credito Pessoal Consignado");
		assertThat(loan.type()).isEqualTo("CREDITO_PESSOAL_COM_CONSIGNACAO");
		assertThat(loan.kind()).isEqualTo("LOAN");
		assertThat(loan.contractAmount()).isEqualByComparingTo("50000");
		assertThat(loan.currencyCode()).isEqualTo("BRL");
	}

	@Test
	void keepsTheDayOfDatesThatCarryZ() {
		PluggyLoan loan = loan();

		assertThat(loan.contractDate().toLocalDate()).isEqualTo(LocalDate.of(2022, 8, 1));
		assertThat(loan.dueDate().toLocalDate()).isEqualTo(LocalDate.of(2028, 1, 15));
	}

	@Test
	void readsInstallmentsAndPayments() {
		PluggyLoan loan = loan();

		assertThat(loan.installments().totalNumberOfInstallments()).isEqualTo(120);
		assertThat(loan.installments().paidInstallments()).isEqualTo(65);
		assertThat(loan.installments().contractRemainingNumber()).isEqualTo(55);
		assertThat(loan.installments().pastDueInstallments()).isZero();
		assertThat(loan.payments().contractOutstandingBalance()).isEqualByComparingTo("25000");
		assertThat(loan.payments().releasesOrEmpty()).hasSize(2);
		PluggyLoan.Release regular = loan.payments().releasesOrEmpty().get(0);
		assertThat(regular.isOverParcelPayment()).isFalse();
		assertThat(regular.paidDate().toLocalDate()).isEqualTo(LocalDate.of(2023, 7, 15));
		assertThat(regular.paidAmount()).isEqualByComparingTo("680.5");
		assertThat(loan.payments().releasesOrEmpty().get(1).isOverParcelPayment()).isTrue();
	}

	@Test
	void toleratesMissingOptionalBlocks() {
		PluggyPage<PluggyLoan> page = mapper.readValue("""
				{ "results": [ { "id": "l1", "itemId": "i1", "productName": "Cheque especial",
				    "kind": "UNARRANGED_ACCOUNT_OVERDRAFT", "currencyCode": "BRL" } ] }
				""", new TypeReference<>() {
		});

		PluggyLoan loan = page.resultsOrEmpty().get(0);
		assertThat(loan.installments()).isNull();
		assertThat(loan.payments()).isNull();
		assertThat(loan.contractAmount()).isNull();
		assertThat(loan.dueDate()).isNull();
	}

	private PluggyLoan loan() {
		PluggyPage<PluggyLoan> page = mapper.readValue(DOC_EXAMPLE, new TypeReference<>() {
		});
		return page.resultsOrEmpty().get(0);
	}

}
