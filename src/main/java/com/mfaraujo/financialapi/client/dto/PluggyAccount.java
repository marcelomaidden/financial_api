package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Conta da Pluggy ({@code GET /accounts}). Cartão de crédito tem {@code type = CREDIT}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyAccount(
		String id,
		String itemId,
		String type,
		String subtype,
		String number,
		String name,
		String marketingName,
		BigDecimal balance,
		String currencyCode,
		CreditData creditData) {

	public static final String TYPE_CREDIT = "CREDIT";

	public boolean isCreditCard() {
		return TYPE_CREDIT.equals(type);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CreditData(
			String level,
			String brand,
			LocalDate balanceCloseDate,
			LocalDate balanceDueDate,
			BigDecimal availableCreditLimit,
			BigDecimal creditLimit,
			BigDecimal minimumPayment,
			String status) {
	}

}
