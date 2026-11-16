package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.mfaraujo.financialapi.domain.enums.LoanKindEnum;

/**
 * Empréstimo ou financiamento de um dono. Só {@code id}, {@code itemId},
 * {@code productName}, {@code kind} e {@code currencyCode} são garantidos. Os demais
 * campos ficam nulos quando a instituição não os informa.
 *
 * @param type tipo do Open Finance, como texto (há dezenas de valores)
 * @param dueDate fim do contrato
 * @param outstandingBalance saldo para quitar o contrato
 * @param lastPayment pagamento regular mais recente, sem contar pagamento avulso
 */
public record Loan(
		String id,
		String itemId,
		String contractNumber,
		String productName,
		LoanKindEnum kind,
		String type,
		BigDecimal contractAmount,
		String currencyCode,
		LocalDate contractDate,
		LocalDate dueDate,
		Installments installments,
		BigDecimal outstandingBalance,
		Payment lastPayment) {

	/** Parcelas do contrato. Cada campo é nulo quando a instituição não o informa. */
	public record Installments(Integer total, Integer paid, Integer remaining, Integer pastDue) {
	}

	public record Payment(LocalDate paidDate, BigDecimal paidAmount) {
	}

}
