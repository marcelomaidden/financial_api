package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fatura de cartão exposta pela API. */
public record BillResponse(
		String id,
		LocalDateTime dueDate,
		LocalDateTime closingDate,
		BigDecimal totalAmount,
		String currencyCode,
		BigDecimal minimumPaymentAmount) {
}
