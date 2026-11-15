package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Transação da Pluggy ({@code GET /v2/transactions}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyTransaction(
		String id,
		String accountId,
		OffsetDateTime date,
		String description,
		BigDecimal amount,
		String currencyCode,
		String type,
		String status,
		String category,
		BigDecimal balance,
		BigDecimal amountInAccountCurrency,
		CreditCardMetadata creditCardMetadata) {

	/**
	 * {@code billId} só vem preenchido quando a compra já entrou em uma fatura. Os campos de
	 * parcela só vêm em compra parcelada.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CreditCardMetadata(String billId, Integer installmentNumber, Integer totalInstallments) {
	}

}
