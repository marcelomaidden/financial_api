package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Transação exposta pela API. */
public record TransactionResponse(
		String id,
		OffsetDateTime date,
		String description,
		BigDecimal amount,
		String currencyCode,
		String type,
		String status,
		String category) {
}
