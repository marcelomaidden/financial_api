package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.resource.dto.ExpenseTransaction;
import com.mfaraujo.financialapi.resource.dto.ExpensesSummary;
import com.mfaraujo.financialapi.resource.dto.Installment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class ExpensesSummaryServiceTest {

	private static final ExpensesPeriod PERIOD = new ExpensesPeriod(LocalDate.of(2026, 11, 1),
			LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 15));

	/** Sem fatura: dia 1 do mês anterior, meia-noite em São Paulo (UTC-3). */
	private static final Instant FALLBACK_SINCE = Instant.parse("2026-10-01T03:00:00Z");

	/** Fatura que fecha em 4/11: fechamento anterior menos 10 dias é 25/10. */
	private static final Instant CYCLE_SINCE = Instant.parse("2026-10-25T03:00:00Z");

	private BillService billService;

	private TransactionService transactionService;

	private ExpensesSummaryService service;

	@BeforeEach
	void setUp() {
		billService = mock(BillService.class);
		transactionService = mock(TransactionService.class);
		service = new ExpensesSummaryService(billService, transactionService);
		when(billService.getAllBills(anyString(), anyString())).thenReturn(List.of());
		when(transactionService.getAllCreatedSince(anyString(), anyString(), any())).thenReturn(List.of());
	}

	// ---- owed

	@Test
	void includesBillThatDueAfterTodayInTheSameMonth() {
		bills("marcelo", "c1", bill("b1", LocalDateTime.of(2026, 11, 20, 0, 0), "250.40", "BRL"),
				bill("b2", LocalDateTime.of(2026, 11, 5, 0, 0), "100", "BRL"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.owed().detail().get("marcelo")).extracting("id").containsExactly("b1", "b2");
		assertThat(expenses.owed().total().value()).isEqualByComparingTo("350.40");
		assertThat(expenses.owed().total().currencyCode()).isEqualTo("BRL");
		assertThat(expenses.owed().detail().get("marcelo").get(0).accountId()).isEqualTo("c1");
		assertThat(expenses.owed().detail().get("marcelo").get(0).name()).isEqualTo("Nubank");
	}

	@Test
	void excludesBillsOutsideThePeriodAndInOtherCurrency() {
		bills("marcelo", "c1", bill("before", LocalDateTime.of(2026, 10, 31, 23, 0), "10", "BRL"),
				bill("after", LocalDateTime.of(2026, 12, 1, 0, 0), "20", "BRL"),
				bill("usd", LocalDateTime.of(2026, 11, 10, 0, 0), "30", "USD"),
				bill("first", LocalDateTime.of(2026, 11, 1, 0, 0), "40", "BRL"),
				bill("last", LocalDateTime.of(2026, 11, 30, 0, 0), "50", "BRL"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.owed().detail().get("marcelo")).extracting("id").containsExactly("first", "last");
		assertThat(expenses.owed().total().value()).isEqualByComparingTo("90");
	}

	@Test
	void ignoresNonCardAccountsAndCardsInOtherCurrencies() {
		Account bank = new Account("b1", "i", AccountTypesEnum.BANK, null, "Conta", null, BigDecimal.TEN, "BRL", null);
		Account usdCard = new Account("u1", "i", AccountTypesEnum.CREDIT, null, "Cartão USD", null, BigDecimal.TEN,
				"USD", null);

		ExpensesSummary expenses = summarize(owner("marcelo", bank, usdCard));

		verify(billService, never()).getAllBills(anyString(), anyString());
		verify(transactionService, never()).getAllCreatedSince(anyString(), anyString(), any());
		assertThat(expenses.owed().detail().get("marcelo")).isEmpty();
		assertThat(expenses.transactions().detail().get("marcelo")).isEmpty();
	}

	// ---- compras: filtros e campos

	@Test
	void keepsOnlyPendingDebitsWithoutBillId() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("ok", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", null, "2026-11-10"),
				tx("billed", TransactionTypesEnum.DEBIT, "PENDING", "bill-1", "BRL", "40", null, "2026-11-10"),
				tx("posted", TransactionTypesEnum.DEBIT, "POSTED", null, "BRL", "50", null, "2026-11-10"),
				tx("credit", TransactionTypesEnum.CREDIT, "PENDING", null, "BRL", "60", null, "2026-11-10"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id").containsExactly("ok");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("30");
		assertThat(expenses.transactions().total().currencyCode()).isEqualTo("BRL");
	}

	@Test
	void mapsPurchaseFieldsAndOmitsAmountInAccountCurrencyAndInstallmentWhenNotApplicable() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("t1", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", "99", "2026-11-10"));

		ExpenseTransaction purchase = summarize(owner("marcelo", card("c1", "Nubank"))).transactions().detail()
				.get("marcelo").get(0);

		assertThat(purchase.id()).isEqualTo("t1");
		assertThat(purchase.accountId()).isEqualTo("c1");
		assertThat(purchase.description()).isEqualTo("Compra t1");
		assertThat(purchase.currencyCode()).isEqualTo("BRL");
		assertThat(purchase.amount()).isEqualByComparingTo("30");
		assertThat(purchase.date()).isNotNull();
		assertThat(purchase.category()).isEqualTo("Shopping");
		assertThat(purchase.amountInAccountCurrency()).isNull();
		assertThat(purchase.installment()).isNull();
	}

	@Test
	void foreignPurchaseCarriesAmountInAccountCurrencyAndTotalUsesIt() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("usd", TransactionTypesEnum.DEBIT, "PENDING", null, "USD", "58", "298.19", "2026-11-10"),
				tx("brl", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "10", null, "2026-11-10"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		ExpenseTransaction usd = expenses.transactions().detail().get("marcelo").get(0);
		assertThat(usd.currencyCode()).isEqualTo("USD");
		assertThat(usd.amount()).isEqualByComparingTo("58");
		assertThat(usd.amountInAccountCurrency()).isEqualByComparingTo("298.19");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("308.19");
	}

	@Test
	void foreignPurchaseWithoutAccountCurrencyAmountStaysOutOfTheTotal() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("usd", TransactionTypesEnum.DEBIT, "PENDING", null, "USD", "58", null, "2026-11-10"),
				tx("brl", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "10", null, "2026-11-10"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id").containsExactly("usd", "brl");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("10");
	}

	// ---- compras: parcelas

	@Test
	void installmentWithAnOldPurchaseDateIsIncludedAndCarriesTheInstallment() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				installmentTx("p3", "2026-08-15", new Transaction.Installment(3, 10)),
				tx("avista", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "10", null, "2026-11-10"));

		List<ExpenseTransaction> detail = summarize(owner("marcelo", card("c1", "Nubank"))).transactions().detail()
				.get("marcelo");

		assertThat(detail).extracting("id").containsExactly("p3", "avista");
		assertThat(detail.get(0).installment()).isEqualTo(new Installment(3, 10));
		assertThat(detail.get(1).installment()).isNull();
	}

	// ---- compras: janela por cartão

	@Test
	void windowComesFromTheLatestBillOfEachCard() {
		bills("marcelo", "c1", cycleBill("2026-11-04", "2026-11-16"));
		// segundo cartão: fecha 20/10 e vence 6/11, então o próximo fechamento é 20/11 e o vencimento 7/12
		bills("marcelo", "c2", cycleBill("2026-10-20", "2026-11-06"));

		summarize(owner("marcelo", card("c1", "Nubank"), card("c2", "Itaú")));

		verify(transactionService).getAllCreatedSince("marcelo", "c1", Instant.parse("2026-10-25T03:00:00Z"));
		verify(transactionService).getAllCreatedSince("marcelo", "c2", Instant.parse("2026-10-10T03:00:00Z"));
	}

	@Test
	void upperLimitIsTheOpenBillDueDatePlusSlack() {
		bills("marcelo", "c1", cycleBill("2026-11-04", "2026-11-16"));
		purchases("marcelo", "c1", CYCLE_SINCE,
				tx("hoje", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "1", null, "2026-11-15"),
				tx("noVencimento", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "2", null, "2026-12-16"),
				tx("noLimite", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "3", null, "2026-12-21"),
				tx("depoisDoLimite", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "4", null, "2026-12-22"),
				tx("fatiaFutura", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "5", null, "2027-01-16"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id")
				.containsExactly("hoje", "noVencimento", "noLimite");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("6");
	}

	@Test
	void cardWithoutBillsUsesTheDefaultWindowUntilToday() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("hoje", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "1", null, "2026-11-15"),
				tx("amanha", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "2", null, "2026-11-16"));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id").containsExactly("hoje");
	}

	@Test
	void purchaseWithoutDateIsKept() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				new Transaction("semData", null, "Compra", BigDecimal.TEN, "BRL", TransactionTypesEnum.DEBIT,
						"PENDING", null, null, null, null));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id").containsExactly("semData");
	}

	// ---- falhas

	@Test
	void billFailureDropsOwnerFromOwedAndPurchasesUseTheDefaultWindow() {
		when(billService.getAllBills(eq("marcelo"), eq("c1")))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("t1", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", null, "2026-11-10"));
		bills("luana", "l1", bill("b1", LocalDateTime.of(2026, 11, 20, 0, 0), "70", "BRL"));

		SectionResult<ExpensesSummary> result = service.summarize(
				List.of(owner("marcelo", card("c1", "Nubank")), owner("luana", card("l1", "Inter"))), PERIOD);

		assertThat(result.errors()).singleElement().satisfies(e -> assertThat(e.owner()).isEqualTo("marcelo"));
		assertThat(result.section().owed().detail()).containsOnlyKeys("luana");
		assertThat(result.section().transactions().detail()).containsOnlyKeys("marcelo", "luana");
		assertThat(result.section().owed().total().value()).isEqualByComparingTo("70");
		assertThat(result.section().transactions().total().value()).isEqualByComparingTo("30");
	}

	@Test
	void billFailureOfOneCardDropsTheOwnerFromOwedButKeepsTheOtherCardWindow() {
		bills("marcelo", "c1", cycleBill("2026-11-04", "2026-11-16"));
		when(billService.getAllBills(eq("marcelo"), eq("c2")))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		SectionResult<ExpensesSummary> result = service.summarize(
				List.of(owner("marcelo", card("c1", "Nubank"), card("c2", "Itaú"))), PERIOD);

		assertThat(result.section().owed().detail()).isEmpty();
		verify(transactionService).getAllCreatedSince("marcelo", "c1", CYCLE_SINCE);
		verify(transactionService).getAllCreatedSince("marcelo", "c2", FALLBACK_SINCE);
	}

	@Test
	void purchaseFailureDropsOwnerOnlyFromTransactions() {
		bills("marcelo", "c1", bill("b1", LocalDateTime.of(2026, 11, 20, 0, 0), "100", "BRL"));
		when(transactionService.getAllCreatedSince(eq("marcelo"), eq("c1"), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		SectionResult<ExpensesSummary> result = service.summarize(List.of(owner("marcelo", card("c1", "Nubank"))),
				PERIOD);

		assertThat(result.errors()).singleElement().satisfies(e -> assertThat(e.owner()).isEqualTo("marcelo"));
		assertThat(result.section().owed().detail()).containsOnlyKeys("marcelo");
		assertThat(result.section().transactions().detail()).isEmpty();
		assertThat(result.section().transactions().total().value()).isEqualByComparingTo("0");
	}

	@Test
	void partialPurchasesOfAFailingOwnerDoNotLeakIntoTheTotal() {
		purchases("marcelo", "c1", FALLBACK_SINCE,
				tx("t1", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", null, "2026-11-10"));
		when(transactionService.getAllCreatedSince(eq("marcelo"), eq("c2"), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		SectionResult<ExpensesSummary> result = service.summarize(
				List.of(owner("marcelo", card("c1", "Nubank"), card("c2", "Itaú"))), PERIOD);

		assertThat(result.section().transactions().detail()).isEmpty();
		assertThat(result.section().transactions().total().value()).isEqualByComparingTo("0");
	}

	// ---- helpers

	private ExpensesSummary summarize(OwnerAccountsResult... owners) {
		return service.summarize(List.of(owners), PERIOD).section();
	}

	private void bills(String owner, String accountId, Bill... bills) {
		when(billService.getAllBills(owner, accountId)).thenReturn(List.of(bills));
	}

	private void purchases(String owner, String accountId, Instant since, Transaction... txs) {
		when(transactionService.getAllCreatedSince(owner, accountId, since)).thenReturn(List.of(txs));
	}

	private static OwnerAccountsResult owner(String name, Account... accounts) {
		return new OwnerAccountsResult(name, List.of(accounts), null);
	}

	private static Account card(String id, String name) {
		return new Account(id, "item", AccountTypesEnum.CREDIT, null, name, null, BigDecimal.TEN, "BRL", null);
	}

	private static Bill bill(String id, LocalDateTime due, String total, String currency) {
		return new Bill(id, due, due.minusDays(7), new BigDecimal(total), currency, null);
	}

	/** Fatura com as datas de fechamento e vencimento definidas, vencendo fora do período de {@code owed}. */
	private static Bill cycleBill(String closing, String due) {
		return new Bill("cycle", LocalDate.parse(due).atStartOfDay(), LocalDate.parse(closing).atStartOfDay(),
				new BigDecimal("1"), "BRL", null);
	}

	private static Transaction tx(String id, TransactionTypesEnum type, String status, String billId, String currency,
			String amount, String inAccountCurrency, String date) {
		return new Transaction(id, OffsetDateTime.parse(date + "T00:00:00Z"), "Compra " + id, new BigDecimal(amount),
				currency, type, status, "Shopping", billId,
				inAccountCurrency == null ? null : new BigDecimal(inAccountCurrency), null);
	}

	private static Transaction installmentTx(String id, String date, Transaction.Installment installment) {
		return new Transaction(id, OffsetDateTime.parse(date + "T00:00:00Z"), "Compra " + id, new BigDecimal("100"),
				"BRL", TransactionTypesEnum.DEBIT, "PENDING", "Shopping", null, null, installment);
	}

}
