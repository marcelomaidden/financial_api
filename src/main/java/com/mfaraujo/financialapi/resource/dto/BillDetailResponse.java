package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Detalhe de uma fatura de cartão com as compras dela. As datas saem em {@code yyyy-mm-dd}.
 * Os campos que a Pluggy não informa saem nulos, {@code installment} só aparece em compra
 * parcelada e {@code amountInAccountCurrency} só aparece em compra fora de BRL.
 */
public record BillDetailResponse(
		String owner,
		String accountId,
		BillSummary bill,
		List<BillTransaction> transactions) {

	public record BillSummary(
			String id,
			LocalDate dueDate,
			LocalDate closingDate,
			BigDecimal totalAmount,
			String currencyCode,
			BigDecimal minimumPaymentAmount,
			Boolean allowsInstallments,
			List<FinanceChargeResponse> financeCharges,
			List<PaymentResponse> payments) {
	}

	public record FinanceChargeResponse(String id, String type, BigDecimal amount, String currencyCode,
			String additionalInfo) {
	}

	public record PaymentResponse(String id, String valueType, LocalDate paymentDate, String paymentMode,
			BigDecimal amount, String currencyCode) {
	}

	public record BillTransaction(
			String id,
			LocalDate date,
			String description,
			BigDecimal amount,
			String currencyCode,
			String category,
			String type,
			String status,
			@JsonInclude(JsonInclude.Include.NON_NULL) Installment installment,
			@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal amountInAccountCurrency) {
	}

}
