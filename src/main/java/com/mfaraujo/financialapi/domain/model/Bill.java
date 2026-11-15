package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fatura de cartão. As datas vêm sem fuso. */
public record Bill(
		String id,
		LocalDateTime dueDate,
		LocalDateTime closingDate,
		BigDecimal totalAmount,
		String currencyCode,
		BigDecimal minimumPaymentAmount) {
}
