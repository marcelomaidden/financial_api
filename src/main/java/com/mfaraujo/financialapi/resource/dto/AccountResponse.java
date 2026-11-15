package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;

/** Conta exposta pela API. */
public record AccountResponse(
		String id,
		String itemId,
		String type,
		String subtype,
		String name,
		String number,
		BigDecimal balance,
		String currencyCode) {
}
