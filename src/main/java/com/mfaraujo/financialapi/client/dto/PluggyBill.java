package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Fatura de cartão da Pluggy ({@code GET /bills}). As datas vêm sem fuso. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyBill(
		String id,
		LocalDateTime dueDate,
		LocalDateTime billClosingDate,
		BigDecimal totalAmount,
		String totalAmountCurrencyCode,
		BigDecimal minimumPaymentAmount) {
}
