package com.mfaraujo.financialapi.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class BillDetailTest {

	@Test
	void windowComesFromTheClosingDate() {
		assertThat(bill("2026-11-15", "2026-11-05").transactionsWindow())
				.contains(new BillDetail.DateRange(date("2026-09-25"), date("2026-11-15")));
	}

	@Test
	void withoutClosingTheWindowComesFromTheDueDate() {
		assertThat(bill("2026-11-15", null).transactionsWindow())
				.contains(new BillDetail.DateRange(date("2026-09-25"), date("2026-11-15")));
	}

	@Test
	void aClosingOnTheLastDayOfTheMonthGoesBackToTheEndOfTheShorterMonth() {
		assertThat(bill("2026-04-10", "2026-03-31").transactionsWindow())
				.contains(new BillDetail.DateRange(date("2026-02-18"), date("2026-04-10")));
	}

	@Test
	void withoutBothDatesThereIsNoWindow() {
		assertThat(bill(null, null).transactionsWindow()).isEmpty();
	}

	private static BillDetail bill(String due, String closing) {
		return new BillDetail("b1", due == null ? null : date(due), closing == null ? null : date(closing),
				BigDecimal.TEN, "BRL", null, null, List.of(), List.of());
	}

	private static LocalDate date(String value) {
		return LocalDate.parse(value);
	}

}
