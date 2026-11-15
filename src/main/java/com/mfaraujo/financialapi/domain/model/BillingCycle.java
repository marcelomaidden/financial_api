package com.mfaraujo.financialapi.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Ciclo da fatura aberta de um cartão, derivado da fatura mais recente que a
 * instituição devolveu. A Pluggy não devolve a fatura aberta, então o ciclo dela é
 * projetado mês a mês a partir do fechamento da fatura conhecida.
 * <p>
 * As datas das faturas chegam à meia-noite UTC e são tratadas como dia, sem conversão
 * de fuso.
 *
 * @param previousClosing fechamento anterior ao da fatura aberta
 * @param nextClosing primeiro fechamento depois de hoje, ou seja, o da fatura aberta
 * @param nextDue vencimento da fatura aberta: o próximo fechamento mais a distância
 * entre fechamento e vencimento da fatura conhecida
 */
public record BillingCycle(LocalDate previousClosing, LocalDate nextClosing, LocalDate nextDue) {

	/**
	 * Ciclo da fatura aberta em {@code today}, ou vazio quando nenhuma fatura tem as
	 * datas de fechamento e vencimento.
	 */
	public static Optional<BillingCycle> from(List<Bill> bills, LocalDate today) {
		return bills.stream()
				.filter(bill -> bill.closingDate() != null && bill.dueDate() != null)
				.max(Comparator.comparing(Bill::closingDate))
				.map(bill -> project(bill.closingDate().toLocalDate(), bill.dueDate().toLocalDate(), today));
	}

	private static BillingCycle project(LocalDate closing, LocalDate due, LocalDate today) {
		long daysToDue = ChronoUnit.DAYS.between(closing, due);
		// Sempre a partir do fechamento conhecido, para o ajuste de fim de mês não acumular.
		int months = (int) Math.max(0, ChronoUnit.MONTHS.between(closing, today));
		while (!closing.plusMonths(months).isAfter(today)) {
			months++;
		}
		LocalDate nextClosing = closing.plusMonths(months);
		LocalDate previousClosing = months == 0 ? closing.minusMonths(1) : closing.plusMonths(months - 1);
		return new BillingCycle(previousClosing, nextClosing, nextClosing.plusDays(daysToDue));
	}

}
