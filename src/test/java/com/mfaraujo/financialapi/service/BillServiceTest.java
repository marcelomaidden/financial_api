package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.domain.model.Bill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillServiceTest {

	private PluggyClient client;

	private BillService service;

	@BeforeEach
	void setUp() {
		client = mock(PluggyClient.class);
		service = new BillService(client, mock(OwnerRegistry.class));
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

	private static PluggyBill bill(String id) {
		return new PluggyBill(id, LocalDateTime.of(2026, 11, 20, 0, 0), LocalDateTime.of(2026, 11, 13, 0, 0),
				new BigDecimal("100"), "BRL", new BigDecimal("10"));
	}

}
