package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Empréstimo da Pluggy ({@code GET /loans}). Só os campos que o sistema usa. As datas
 * chegam à meia-noite UTC e são tratadas como dia.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyLoan(
		String id,
		String itemId,
		String contractNumber,
		String productName,
		String type,
		String kind,
		BigDecimal contractAmount,
		String currencyCode,
		LocalDateTime contractDate,
		LocalDateTime dueDate,
		Installments installments,
		Payments payments) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Installments(
			Integer totalNumberOfInstallments,
			Integer paidInstallments,
			Integer contractRemainingNumber,
			Integer pastDueInstallments) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Payments(BigDecimal contractOutstandingBalance, List<Release> releases) {

		public List<Release> releasesOrEmpty() {
			return releases == null ? List.of() : releases;
		}

	}

	/** Pagamento feito. {@code isOverParcelPayment} é verdadeiro no pagamento avulso. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Release(Boolean isOverParcelPayment, LocalDateTime paidDate, BigDecimal paidAmount) {
	}

}
