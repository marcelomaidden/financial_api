package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Fatura de cartão da Pluggy ({@code GET /bills}). As datas chegam à meia-noite UTC
 * (por exemplo {@code 2026-11-16T00:00:00.000Z}) e são tratadas como dia, sem conversão de fuso.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyBill(
		String id,
		LocalDateTime dueDate,
		LocalDateTime billClosingDate,
		BigDecimal totalAmount,
		String totalAmountCurrencyCode,
		BigDecimal minimumPaymentAmount) {
}
