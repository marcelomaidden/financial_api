package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cartão de crédito exposto pela API. Os campos de crédito são nulos quando a Pluggy não os informa. */
public record CreditCardResponse(
		String id,
		String itemId,
		String name,
		String number,
		String brand,
		BigDecimal balance,
		String currencyCode,
		BigDecimal creditLimit,
		BigDecimal availableCreditLimit,
		LocalDate balanceCloseDate,
		LocalDate balanceDueDate) {
}
