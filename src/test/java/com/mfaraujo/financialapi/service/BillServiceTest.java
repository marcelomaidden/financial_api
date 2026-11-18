package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.client.dto.PluggyBillDetail;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.BillDetailResult;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillServiceTest {

	/** Janela da fatura de {@link #detail()}: vence em 2026-11-20 e não tem fechamento. */
	private static final LocalDate DUE_FROM = LocalDate.of(2026, 9, 30);

	private static final LocalDate DUE_TO = LocalDate.of(2026, 11, 20);

	private PluggyClient client;

	private BillService service;

	private OwnerRegistry owners;

	private TransactionService transactions;

	@BeforeEach
	void setUp() {
		client = mock(PluggyClient.class);
		owners = mock(OwnerRegistry.class);
		transactions = mock(TransactionService.class);
		service = new BillService(client, owners, transactions);
	}

	@Test
	void returnsSinglePage() {
		when(client.listBills("marcelo", "c1", 1, 500)).thenReturn(new PluggyPage<>(1, 1, 1, List.of(bill("b1"))));

		List<Bill> all = service.getAllBills("marcelo", "c1");

		assertThat(all).extracting(Bill::id).containsExactly("b1");
		verify(client, times(1)).listBills("marcelo", "c1", 1, 500);
	}

	@Test
	void followsPagesUntilTheLast() {
		when(client.listBills("marcelo", "c1", 1, 500)).thenReturn(new PluggyPage<>(1, 3, 3, List.of(bill("b1"))));
		when(client.listBills("marcelo", "c1", 2, 500)).thenReturn(new PluggyPage<>(2, 3, 3, List.of(bill("b2"))));
		when(client.listBills("marcelo", "c1", 3, 500)).thenReturn(new PluggyPage<>(3, 3, 3, List.of(bill("b3"))));

		List<Bill> all = service.getAllBills("marcelo", "c1");

		assertThat(all).extracting(Bill::id).containsExactly("b1", "b2", "b3");
	}

	@Test
	void returnsEmptyListForEmptyPage() {
		when(client.listBills("marcelo", "c1", 1, 500)).thenReturn(new PluggyPage<>(1, 0, 0, null));

		assertThat(service.getAllBills("marcelo", "c1")).isEmpty();
		verify(client, times(1)).listBills("marcelo", "c1", 1, 500);
	}

	@Test
	void stopsWhenTotalPagesIsMissing() {
		when(client.listBills("marcelo", "c1", 1, 500)).thenReturn(new PluggyPage<>(1, null, null, List.of(bill("b1"))));

		assertThat(service.getAllBills("marcelo", "c1")).hasSize(1);
		verify(client, times(1)).listBills("marcelo", "c1", 1, 500);
	}

	@Test
	void detailOfAnUnknownOwnerDoesNotCallPluggy() {
		doThrow(new UnknownOwnerException("fulano")).when(owners).require("fulano");

		assertThatThrownBy(() -> service.getBillDetail("fulano", "b1", "c1")).isInstanceOf(UnknownOwnerException.class);
		verify(client, never()).retrieveBill(any(), any());
		verify(transactions, never()).getAllTransactions(any(), any(), any(), any());
	}

	@Test
	void detailOfAMissingBillDoesNotFetchPurchases() {
		when(client.retrieveBill("marcelo", "b1"))
				.thenThrow(new PluggyException("marcelo", HttpStatus.NOT_FOUND, "Fatura não encontrada"));

		assertThatThrownBy(() -> service.getBillDetail("marcelo", "b1", "c1")).isInstanceOf(PluggyException.class);
		verify(transactions, never()).getAllTransactions(any(), any(), any(), any());
	}

	@Test
	void detailFailsWhenPluggyFailsOnTheBill() {
		when(client.retrieveBill("marcelo", "b1"))
				.thenThrow(new PluggyException("marcelo", HttpStatus.BAD_GATEWAY, "falhou"));

		assertThatThrownBy(() -> service.getBillDetail("marcelo", "b1", "c1")).isInstanceOf(PluggyException.class);
	}

	@Test
	void detailFailsWhenPluggyFailsOnThePurchases() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		when(transactions.getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO))
				.thenThrow(new PluggyException("marcelo", HttpStatus.BAD_GATEWAY, "falhou"));

		assertThatThrownBy(() -> service.getBillDetail("marcelo", "b1", "c1")).isInstanceOf(PluggyException.class);
	}

	@Test
	void detailSortsPurchasesByDateDescendingKeepingPluggyOrderOnTiesAndNullDatesLast() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		when(transactions.getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO)).thenReturn(List.of(
				tx("old", "2026-11-01T10:00:00Z"), tx("nodate", null), tx("tie1", "2026-11-10T10:00:00Z"),
				tx("new", "2026-11-12T10:00:00Z"), tx("tie2", "2026-11-10T10:00:00Z")));

		BillDetailResult result = service.getBillDetail("marcelo", "b1", "c1");

		assertThat(result.bill().id()).isEqualTo("b1");
		assertThat(result.transactions()).extracting(Transaction::id)
				.containsExactly("new", "tie1", "tie2", "old", "nodate");
	}

	@Test
	void detailKeepsTheInstallmentOfPurchases() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		Transaction installment = new Transaction("t1", OffsetDateTime.parse("2026-11-10T10:00:00Z"), "d",
				new BigDecimal("10"), "BRL", TransactionTypesEnum.DEBIT, "POSTED", null, "b1", null,
				new Transaction.Installment(3, 10));
		when(transactions.getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO)).thenReturn(List.of(installment, tx("t2", null)));

		BillDetailResult result = service.getBillDetail("marcelo", "b1", "c1");

		assertThat(result.transactions().get(0).installment()).isEqualTo(new Transaction.Installment(3, 10));
		assertThat(result.transactions().get(1).installment()).isNull();
	}

	@Test
	void detailOfABillWithoutPurchasesHasAnEmptyList() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		when(transactions.getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO)).thenReturn(List.of());

		assertThat(service.getBillDetail("marcelo", "b1", "c1").transactions()).isEmpty();
	}

	@Test
	void detailSearchesTheWindowFromTheClosingDate() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(new PluggyBillDetail("b1",
				LocalDateTime.of(2026, 11, 15, 0, 0), LocalDateTime.of(2026, 11, 5, 0, 0), new BigDecimal("100"), "BRL",
				null, null, null, null));
		when(transactions.getAllTransactions(any(), any(), any(), any())).thenReturn(List.of());

		service.getBillDetail("marcelo", "b1", "c1");

		verify(transactions).getAllTransactions("marcelo", "c1", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 11, 15));
	}

	@Test
	void detailSearchesTheWindowFromTheDueDateWithoutClosing() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		when(transactions.getAllTransactions(any(), any(), any(), any())).thenReturn(List.of());

		service.getBillDetail("marcelo", "b1", "c1");

		verify(transactions).getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO);
	}

	@Test
	void detailOfABillWithoutDatesSearchesWithoutWindowAndStillFilters() {
		when(client.retrieveBill("marcelo", "b1"))
				.thenReturn(new PluggyBillDetail("b1", null, null, new BigDecimal("100"), "BRL", null, null, null, null));
		when(transactions.getAllTransactions("marcelo", "c1", null, null))
				.thenReturn(List.of(tx("mine", "2026-11-10T10:00:00Z"), txOfBill("other", "b2")));

		BillDetailResult result = service.getBillDetail("marcelo", "b1", "c1");

		assertThat(result.transactions()).extracting(Transaction::id).containsExactly("mine");
	}

	@Test
	void detailKeepsOnlyTransactionsOfTheBill() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		when(transactions.getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO)).thenReturn(List.of(
				tx("mine", "2026-11-10T10:00:00Z"), txOfBill("other", "b2"), txOfBill("pending", null)));

		BillDetailResult result = service.getBillDetail("marcelo", "b1", "c1");

		assertThat(result.transactions()).extracting(Transaction::id).containsExactly("mine");
	}

	@Test
	void detailKeepsAnInstallmentOfAnOldPurchasePostedInTheCycle() {
		when(client.retrieveBill("marcelo", "b1")).thenReturn(detail());
		Transaction second = new Transaction("t2of4", OffsetDateTime.parse("2026-10-21T03:00:00Z"), "Compra de setembro",
				new BigDecimal("25"), "BRL", TransactionTypesEnum.DEBIT, "POSTED", null, "b1", null,
				new Transaction.Installment(2, 4));
		when(transactions.getAllTransactions("marcelo", "c1", DUE_FROM, DUE_TO)).thenReturn(List.of(second));

		BillDetailResult result = service.getBillDetail("marcelo", "b1", "c1");

		assertThat(result.transactions()).containsExactly(second);
	}

	private static Transaction txOfBill(String id, String billId) {
		return new Transaction(id, OffsetDateTime.parse("2026-11-10T10:00:00Z"), "d", new BigDecimal("1"), "BRL",
				TransactionTypesEnum.DEBIT, "POSTED", null, billId, null, null);
	}

	private static PluggyBillDetail detail() {
		return new PluggyBillDetail("b1", LocalDateTime.of(2026, 11, 20, 0, 0), null, new BigDecimal("100"), "BRL",
				null, null, null, null);
	}

	private static Transaction tx(String id, String date) {
		return new Transaction(id, date == null ? null : OffsetDateTime.parse(date), "d", new BigDecimal("1"), "BRL",
				TransactionTypesEnum.DEBIT, "POSTED", null, "b1", null, null);
	}

	private static PluggyBill bill(String id) {
		return new PluggyBill(id, LocalDateTime.of(2026, 11, 20, 0, 0), LocalDateTime.of(2026, 11, 13, 0, 0),
				new BigDecimal("100"), "BRL", new BigDecimal("10"));
	}

}
