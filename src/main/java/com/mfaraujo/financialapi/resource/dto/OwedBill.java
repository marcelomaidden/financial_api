package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fatura de cartão no detalhe de {@code expenses.owed}. */
public record OwedBill(String id, String accountId, String name, LocalDateTime dueDate, BigDecimal totalAmount,
		String currencyCode) {
}
