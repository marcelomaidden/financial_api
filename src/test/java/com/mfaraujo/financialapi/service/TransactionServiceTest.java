package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyCursorPage;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 15);

	private PluggyClient client;

	private TransactionService service;

	@BeforeEach
	void setUp() {
		client = mock(PluggyClient.class);
		service = new TransactionService(client, mock(OwnerRegistry.class));
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

	private static PluggyTransaction pluggyTx(String id) {
		return new PluggyTransaction(id, "a1", OffsetDateTime.parse("2026-11-10T12:00:00Z"), "d",
				new BigDecimal("1"), "BRL", "CREDIT", "POSTED", null, null);
	}

}
