package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Detalhe de uma fatura de cartão. Só {@code id}, {@code dueDate} e {@code totalAmount}
 * vêm sempre da Pluggy. Os demais campos ficam nulos quando ela não os informa, e as
 * listas ficam vazias.
 */
public record BillDetail(
		String id,
		LocalDate dueDate,
		LocalDate closingDate,
		BigDecimal totalAmount,
		String currencyCode,
		BigDecimal minimumPaymentAmount,
		Boolean allowsInstallments,
		List<FinanceCharge> financeCharges,
		List<Payment> payments) {

	public record FinanceCharge(String id, String type, BigDecimal amount, String currencyCode,
			String additionalInfo) {
	}

	public record Payment(String id, String valueType, LocalDate paymentDate, String paymentMode, BigDecimal amount,
			String currencyCode) {
	}

}
