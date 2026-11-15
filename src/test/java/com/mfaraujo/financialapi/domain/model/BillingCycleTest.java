package com.mfaraujo.financialapi.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BillingCycleTest {

	@Test
	void projectsTheOpenBillFromTheRealExample() {
		// fatura real: fecha 4/11, vence 16/11. Hoje é 15/11.
		BillingCycle cycle = BillingCycle.from(List.of(bill("2026-11-04", "2026-11-16")), date("2026-11-15"))
				.orElseThrow();

		assertThat(cycle.previousClosing()).isEqualTo(date("2026-11-04"));
		assertThat(cycle.nextClosing()).isEqualTo(date("2026-12-04"));
		assertThat(cycle.nextDue()).isEqualTo(date("2026-12-16"));
	}

	@Test
	void keepsTheKnownClosingWhenItIsStillInTheFuture() {
		BillingCycle cycle = BillingCycle.from(List.of(bill("2026-11-04", "2026-11-16")), date("2026-11-02"))
				.orElseThrow();

		assertThat(cycle.previousClosing()).isEqualTo(date("2026-10-04"));
		assertThat(cycle.nextClosing()).isEqualTo(date("2026-11-04"));
		assertThat(cycle.nextDue()).isEqualTo(date("2026-11-16"));
	}

	@Test
	void movesToTheNextMonthOnTheClosingDay() {
		BillingCycle cycle = BillingCycle.from(List.of(bill("2026-11-04", "2026-11-16")), date("2026-11-04"))
				.orElseThrow();

		assertThat(cycle.nextClosing()).isEqualTo(date("2026-12-04"));
		assertThat(cycle.previousClosing()).isEqualTo(date("2026-11-04"));
	}

	@Test
	void projectsFromAnOldLatestBill() {
		BillingCycle cycle = BillingCycle.from(List.of(bill("2026-05-04", "2026-05-16")), date("2026-11-15"))
				.orElseThrow();

		assertThat(cycle.previousClosing()).isEqualTo(date("2026-11-04"));
		assertThat(cycle.nextClosing()).isEqualTo(date("2026-12-04"));
		assertThat(cycle.nextDue()).isEqualTo(date("2026-12-16"));
	}

	@Test
	void givesTheSameCycleWhenTheLatestBillIsTheOneThatAlreadyDue() {
		BillingCycle fromClosed = BillingCycle.from(List.of(bill("2026-11-04", "2026-11-16")), date("2026-11-20"))
				.orElseThrow();
		BillingCycle fromOlder = BillingCycle.from(List.of(bill("2026-10-04", "2026-10-16")), date("2026-11-20"))
				.orElseThrow();

		assertThat(fromOlder).isEqualTo(fromClosed);
	}

	@Test
	void doesNotAccumulateMonthEndAdjustments() {
		// fecha dia 31: fevereiro encurta, mas março volta ao dia 31
		List<Bill> bills = List.of(bill("2026-01-31", "2026-02-12"));

		BillingCycle february = BillingCycle.from(bills, date("2026-02-15")).orElseThrow();
		BillingCycle march = BillingCycle.from(bills, date("2026-03-01")).orElseThrow();

		assertThat(february.previousClosing()).isEqualTo(date("2026-01-31"));
		assertThat(february.nextClosing()).isEqualTo(date("2026-02-28"));
		assertThat(february.nextDue()).isEqualTo(date("2026-03-12"));
		assertThat(march.previousClosing()).isEqualTo(date("2026-02-28"));
		assertThat(march.nextClosing()).isEqualTo(date("2026-03-31"));
		assertThat(march.nextDue()).isEqualTo(date("2026-04-12"));
	}

	@Test
	void usesTheBillWithTheLatestClosingDate() {
		BillingCycle cycle = BillingCycle.from(List.of(bill("2026-09-04", "2026-09-20"),
				bill("2026-11-04", "2026-11-16"), bill("2026-10-04", "2026-10-18")), date("2026-11-15"))
				.orElseThrow();

		assertThat(cycle.nextDue()).isEqualTo(date("2026-12-16"));
	}

	@Test
	void ignoresBillsWithoutBothDates() {
		Bill noClosing = new Bill("b1", LocalDateTime.of(2026, 11, 16, 0, 0), null, BigDecimal.TEN, "BRL", null);
		Bill noDue = new Bill("b2", null, LocalDateTime.of(2026, 11, 4, 0, 0), BigDecimal.TEN, "BRL", null);

		assertThat(BillingCycle.from(List.of(noClosing, noDue), date("2026-11-15"))).isEmpty();
		assertThat(BillingCycle.from(List.of(noClosing, bill("2026-10-04", "2026-10-16")), date("2026-11-15")))
				.get().extracting(BillingCycle::nextClosing).isEqualTo(date("2026-12-04"));
	}

	@Test
	void isEmptyWithoutBills() {
		assertThat(BillingCycle.from(List.of(), date("2026-11-15"))).isEmpty();
	}

	private static Bill bill(String closing, String due) {
		return new Bill("b", date(due).atStartOfDay(), date(closing).atStartOfDay(), BigDecimal.TEN, "BRL", null);
	}

	private static LocalDate date(String iso) {
		return LocalDate.parse(iso);
	}

}
