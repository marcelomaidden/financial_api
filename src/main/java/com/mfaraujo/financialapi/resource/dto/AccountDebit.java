package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Saída de dinheiro no detalhe de {@code expenses.accountDebits}. O {@code amount} é sempre positivo. */
public record AccountDebit(String id, String description, BigDecimal amount, String currencyCode,
						   OffsetDateTime date, String accountId) {
}
