package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

	/** Folga, em dias, antes e depois do ciclo da fatura na janela de busca das compras. */
	static final int WINDOW_SLACK_DAYS = 10;

	/** Distância usual entre o fechamento e o vencimento, para estimar o fechamento ausente. */
	static final int DUE_TO_CLOSING_DAYS = 10;

	/**
	 * Janela de {@code date} em que as compras da fatura são buscadas: do fechamento menos
	 * 1 mês e a folga até o fechamento mais a folga. Sem fechamento, o vencimento faz o papel
	 * do fechamento estimado. Vazia sem as duas datas. A janela é generosa porque as compras
	 * são filtradas depois pelo {@code billId}.
	 */
	public Optional<DateRange> transactionsWindow() {
		if (closingDate != null) {
			return Optional.of(new DateRange(closingDate.minusMonths(1).minusDays(WINDOW_SLACK_DAYS),
					closingDate.plusDays(WINDOW_SLACK_DAYS)));
		}
		if (dueDate != null) {
			return Optional.of(new DateRange(
					dueDate.minusMonths(1).minusDays(DUE_TO_CLOSING_DAYS + WINDOW_SLACK_DAYS),
					dueDate.minusDays(DUE_TO_CLOSING_DAYS).plusDays(WINDOW_SLACK_DAYS)));
		}
		return Optional.empty();
	}

	/** Intervalo de dias, com as duas pontas incluídas. */
	public record DateRange(LocalDate from, LocalDate to) {
	}

	public record FinanceCharge(String id, String type, BigDecimal amount, String currencyCode,
			String additionalInfo) {
	}

	public record Payment(String id, String valueType, LocalDate paymentDate, String paymentMode, BigDecimal amount,
			String currencyCode) {
	}

}
