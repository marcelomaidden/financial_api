package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Empréstimo exposto pela API. Só {@code id}, {@code itemId}, {@code productName},
 * {@code kind} e {@code currencyCode} são garantidos. Os demais campos são nulos
 * quando a instituição não os informa.
 */
public record LoanResponse(
		String id,
		String itemId,
		String contractNumber,
		String productName,
		String kind,
		String type,
		BigDecimal contractAmount,
		String currencyCode,
		LocalDate contractDate,
		LocalDate dueDate,
		Installments installments,
		BigDecimal outstandingBalance,
		LastPayment lastPayment) {

	public record Installments(Integer total, Integer paid, Integer remaining, Integer pastDue) {
	}

	public record LastPayment(LocalDate paidDate, BigDecimal paidAmount) {
	}

}
