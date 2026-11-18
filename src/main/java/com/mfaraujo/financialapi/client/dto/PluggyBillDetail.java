package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Detalhe de uma fatura de cartão da Pluggy ({@code GET /bills/{id}}). Só os campos que o
 * sistema usa. As datas chegam à meia-noite e são tratadas como dia, sem conversão de fuso.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyBillDetail(
		String id,
		LocalDateTime dueDate,
		LocalDateTime billClosingDate,
		BigDecimal totalAmount,
		String totalAmountCurrencyCode,
		BigDecimal minimumPaymentAmount,
		Boolean allowsInstallments,
		List<FinanceCharge> financeCharges,
		List<Payment> payments) {

	public List<FinanceCharge> financeChargesOrEmpty() {
		return financeCharges == null ? List.of() : financeCharges;
	}

	public List<Payment> paymentsOrEmpty() {
		return payments == null ? List.of() : payments;
	}

	/** Encargo da fatura, como IOF ou juros. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FinanceCharge(String id, String type, BigDecimal amount, String currencyCode,
			String additionalInfo) {
	}

	/** Pagamento feito na fatura. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Payment(String id, String valueType, LocalDateTime paymentDate, String paymentMode,
			BigDecimal amount, String currencyCode) {
	}

}
