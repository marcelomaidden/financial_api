package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Compra de cartão ainda sem fatura, no detalhe de {@code expenses.transactions}.
 * {@code amountInAccountCurrency} só aparece quando a moeda da compra não é BRL.
 */
public record ExpenseTransaction(
		String id,
		String accountId,
		String description,
		String currencyCode,
		BigDecimal amount,
		OffsetDateTime date,
		String category,
		@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal amountInAccountCurrency) {
}
