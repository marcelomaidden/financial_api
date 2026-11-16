package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Transação da lista de {@code GET /transactions}, com o dono e a conta de origem. */
public record ConsolidatedTransaction(
		String owner,
		String accountId,
		String id,
		OffsetDateTime date,
		String description,
		BigDecimal amount,
		String currencyCode,
		String type,
		String status,
		String category) {
}
