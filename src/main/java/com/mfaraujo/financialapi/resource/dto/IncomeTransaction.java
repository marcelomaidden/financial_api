package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;

/** Entrada de dinheiro no detalhe de {@code incomes.transactions}. */
public record IncomeTransaction(String id, String description, BigDecimal amount, String currencyCode,
		String accountId) {
}
