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
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyCursorPage;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 15);

	private PluggyClient client;

	private TransactionService service;

	private OwnerRegistry owners;

	@BeforeEach
	void setUp() {
		client = mock(PluggyClient.class);
		owners = mock(OwnerRegistry.class);
		service = new TransactionService(client, owners);
	}

	@Test
	void returnsSinglePage() {
		when(client.listTransactions("marcelo", "a1", FROM, TO, null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), null));

		List<Transaction> all = service.getAllTransactions("marcelo", "a1", FROM, TO);

		assertThat(all).extracting(Transaction::id).containsExactly("t1");
		verify(client, times(1)).listTransactions("marcelo", "a1", FROM, TO, null);
	}

	@Test
	void followsCursorAcrossPages() {
		when(client.listTransactions("marcelo", "a1", FROM, TO, null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), "?after=c2"));
		when(client.listTransactions("marcelo", "a1", FROM, TO, "c2"))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t2"), pluggyTx("t3")), "?after=c3"));
		when(client.listTransactions("marcelo", "a1", FROM, TO, "c3"))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t4")), null));

		List<Transaction> all = service.getAllTransactions("marcelo", "a1", FROM, TO);

		assertThat(all).extracting(Transaction::id).containsExactly("t1", "t2", "t3", "t4");
	}

	@Test
	void returnsEmptyListForEmptyPage() {
		when(client.listTransactions("marcelo", "a1", FROM, TO, null))
				.thenReturn(new PluggyCursorPage<>(null, null));

		assertThat(service.getAllTransactions("marcelo", "a1", FROM, TO)).isEmpty();
	}

	@Test
	void createdSinceReturnsSinglePage() {
		Instant since = Instant.parse("2026-10-25T03:00:00Z");
		when(client.listTransactionsCreatedSince("marcelo", "a1", since, null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), null));

		List<Transaction> all = service.getAllCreatedSince("marcelo", "a1", since);

		assertThat(all).extracting(Transaction::id).containsExactly("t1");
		verify(client, times(1)).listTransactionsCreatedSince("marcelo", "a1", since, null);
	}

	@Test
	void createdSinceFollowsCursorAcrossPages() {
		Instant since = Instant.parse("2026-10-25T03:00:00Z");
		when(client.listTransactionsCreatedSince("marcelo", "a1", since, null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), "?createdAtFrom=x&after=c2"));
		when(client.listTransactionsCreatedSince("marcelo", "a1", since, "c2"))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t2")), "?after=c3"));
		when(client.listTransactionsCreatedSince("marcelo", "a1", since, "c3"))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t3")), null));

		List<Transaction> all = service.getAllCreatedSince("marcelo", "a1", since);

		assertThat(all).extracting(Transaction::id).containsExactly("t1", "t2", "t3");
	}

	@Test
	void createdSinceReturnsEmptyListForEmptyPage() {
		Instant since = Instant.parse("2026-10-25T03:00:00Z");
		when(client.listTransactionsCreatedSince("marcelo", "a1", since, null))
				.thenReturn(new PluggyCursorPage<>(null, null));

		assertThat(service.getAllCreatedSince("marcelo", "a1", since)).isEmpty();
	}

	@Test
	void byBillReturnsSinglePage() {
		when(client.listTransactionsByBill("marcelo", "a1", "b1", null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), null));

		assertThat(service.getAllByBill("marcelo", "a1", "b1")).extracting(Transaction::id).containsExactly("t1");
		verify(client, times(1)).listTransactionsByBill("marcelo", "a1", "b1", null);
	}

	@Test
	void byBillFollowsCursorAcrossPages() {
		when(client.listTransactionsByBill("marcelo", "a1", "b1", null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), "?billId=b1&after=c2"));
		when(client.listTransactionsByBill("marcelo", "a1", "b1", "c2"))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t2")), "?after=c3"));
		when(client.listTransactionsByBill("marcelo", "a1", "b1", "c3"))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t3")), null));

		assertThat(service.getAllByBill("marcelo", "a1", "b1")).extracting(Transaction::id)
				.containsExactly("t1", "t2", "t3");
	}

	@Test
	void byBillReturnsEmptyListForEmptyPage() {
		when(client.listTransactionsByBill("marcelo", "a1", "b1", null))
				.thenReturn(new PluggyCursorPage<>(null, null));

		assertThat(service.getAllByBill("marcelo", "a1", "b1")).isEmpty();
	}

	@Test
	void byBillPropagatesAFailureOnALaterPageWithoutPartialResult() {
		when(client.listTransactionsByBill("marcelo", "a1", "b1", null))
				.thenReturn(new PluggyCursorPage<>(List.of(pluggyTx("t1")), "?after=c2"));
		when(client.listTransactionsByBill("marcelo", "a1", "b1", "c2"))
				.thenThrow(new PluggyException("marcelo", HttpStatus.BAD_GATEWAY, "falhou"));

		assertThatThrownBy(() -> service.getAllByBill("marcelo", "a1", "b1")).isInstanceOf(PluggyException.class);
	}

	@Test
	void byBillWithAnUnknownOwnerDoesNotCallPluggy() {
		doThrow(new UnknownOwnerException("fulano")).when(owners).require("fulano");

		assertThatThrownBy(() -> service.getAllByBill("fulano", "a1", "b1")).isInstanceOf(UnknownOwnerException.class);
		verify(client, never()).listTransactionsByBill(any(), any(), any(), any());
	}

	private static PluggyTransaction pluggyTx(String id) {
		return new PluggyTransaction(id, "a1", OffsetDateTime.parse("2026-11-10T12:00:00Z"), "d",
				new BigDecimal("1"), "BRL", "CREDIT", "POSTED", null, null, null, null);
	}

}
