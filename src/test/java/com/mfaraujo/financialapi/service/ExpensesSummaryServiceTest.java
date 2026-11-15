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
import com.mfaraujo.financialapi.resource.dto.ExpensesSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class ExpensesSummaryServiceTest {

	private static final ExpensesPeriod PERIOD = new ExpensesPeriod(LocalDate.of(2026, 11, 1),
			LocalDate.of(2026, 11, 30), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 15));

	private BillService billService;

	private TransactionService transactionService;

	private ExpensesSummaryService service;

	@BeforeEach
	void setUp() {
		billService = mock(BillService.class);
		transactionService = mock(TransactionService.class);
		service = new ExpensesSummaryService(billService, transactionService);
		when(billService.getAllBills(anyString(), anyString())).thenReturn(List.of());
		when(transactionService.getAllTransactions(anyString(), anyString(), any(), any())).thenReturn(List.of());
	}

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
		verify(transactionService, never()).getAllTransactions(anyString(), anyString(), any(), any());
		assertThat(expenses.owed().detail().get("marcelo")).isEmpty();
		assertThat(expenses.transactions().detail().get("marcelo")).isEmpty();
	}

	@Test
	void keepsOnlyPendingDebitsWithoutBillId() {
		purchases("marcelo", "c1",
				tx("ok", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", null),
				tx("billed", TransactionTypesEnum.DEBIT, "PENDING", "bill-1", "BRL", "40", null),
				tx("posted", TransactionTypesEnum.DEBIT, "POSTED", null, "BRL", "50", null),
				tx("credit", TransactionTypesEnum.CREDIT, "PENDING", null, "BRL", "60", null));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id").containsExactly("ok");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("30");
		assertThat(expenses.transactions().total().currencyCode()).isEqualTo("BRL");
	}

	@Test
	void mapsPurchaseFieldsAndOmitsAmountInAccountCurrencyForBrl() {
		purchases("marcelo", "c1", tx("t1", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", "99"));

		var purchase = summarize(owner("marcelo", card("c1", "Nubank"))).transactions().detail().get("marcelo").get(0);

		assertThat(purchase.id()).isEqualTo("t1");
		assertThat(purchase.accountId()).isEqualTo("c1");
		assertThat(purchase.description()).isEqualTo("Compra t1");
		assertThat(purchase.currencyCode()).isEqualTo("BRL");
		assertThat(purchase.amount()).isEqualByComparingTo("30");
		assertThat(purchase.date()).isNotNull();
		assertThat(purchase.category()).isEqualTo("Shopping");
		assertThat(purchase.amountInAccountCurrency()).isNull();
	}

	@Test
	void foreignPurchaseCarriesAmountInAccountCurrencyAndTotalUsesIt() {
		purchases("marcelo", "c1",
				tx("usd", TransactionTypesEnum.DEBIT, "PENDING", null, "USD", "58", "298.19"),
				tx("brl", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "10", null));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		var usd = expenses.transactions().detail().get("marcelo").get(0);
		assertThat(usd.currencyCode()).isEqualTo("USD");
		assertThat(usd.amount()).isEqualByComparingTo("58");
		assertThat(usd.amountInAccountCurrency()).isEqualByComparingTo("298.19");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("308.19");
	}

	@Test
	void foreignPurchaseWithoutAccountCurrencyAmountStaysOutOfTheTotal() {
		purchases("marcelo", "c1",
				tx("usd", TransactionTypesEnum.DEBIT, "PENDING", null, "USD", "58", null),
				tx("brl", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "10", null));

		ExpensesSummary expenses = summarize(owner("marcelo", card("c1", "Nubank")));

		assertThat(expenses.transactions().detail().get("marcelo")).extracting("id").containsExactly("usd", "brl");
		assertThat(expenses.transactions().total().value()).isEqualByComparingTo("10");
	}

	@Test
	void queriesPurchasesWithTheTransactionsPeriod() {
		summarize(owner("marcelo", card("c1", "Nubank")));

		verify(transactionService).getAllTransactions("marcelo", "c1", LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 11, 15));
	}

	@Test
	void billFailureDropsOwnerOnlyFromOwed() {
		when(billService.getAllBills(eq("marcelo"), eq("c1")))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));
		purchases("marcelo", "c1", tx("t1", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", null));
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
	void purchaseFailureDropsOwnerOnlyFromTransactions() {
		bills("marcelo", "c1", bill("b1", LocalDateTime.of(2026, 11, 20, 0, 0), "100", "BRL"));
		when(transactionService.getAllTransactions(eq("marcelo"), eq("c1"), any(), any()))
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
		purchases("marcelo", "c1", tx("t1", TransactionTypesEnum.DEBIT, "PENDING", null, "BRL", "30", null));
		when(transactionService.getAllTransactions(eq("marcelo"), eq("c2"), any(), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		SectionResult<ExpensesSummary> result = service.summarize(
				List.of(owner("marcelo", card("c1", "Nubank"), card("c2", "Itaú"))), PERIOD);

		assertThat(result.section().transactions().detail()).isEmpty();
		assertThat(result.section().transactions().total().value()).isEqualByComparingTo("0");
	}

	private ExpensesSummary summarize(OwnerAccountsResult... owners) {
		return service.summarize(List.of(owners), PERIOD).section();
	}

	private void bills(String owner, String accountId, Bill... bills) {
		when(billService.getAllBills(owner, accountId)).thenReturn(List.of(bills));
	}

	private void purchases(String owner, String accountId, Transaction... txs) {
		when(transactionService.getAllTransactions(owner, accountId, PERIOD.transactionsFrom(),
				PERIOD.transactionsTo())).thenReturn(List.of(txs));
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

	private static Transaction tx(String id, TransactionTypesEnum type, String status, String billId, String currency,
			String amount, String inAccountCurrency) {
		return new Transaction(id, OffsetDateTime.parse("2026-11-10T12:00:00Z"), "Compra " + id,
				new BigDecimal(amount), currency, type, status, "Shopping", billId,
				inAccountCurrency == null ? null : new BigDecimal(inAccountCurrency));
	}

}
