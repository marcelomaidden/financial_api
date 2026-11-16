package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyLoan;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.config.PluggyProperties.Owner;
import com.mfaraujo.financialapi.domain.result.OwnerLoansResult;
import com.mfaraujo.financialapi.exception.AllOwnersFailedException;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class LoanServiceTest {

	private PluggyClient client;

	private OwnerRegistry registry;

	private LoanService service;

	private final Owner marcelo = new Owner("marcelo", "id", "secret", List.of("m-item1", "m-item2"));

	private final Owner luana = new Owner("luana", "id", "secret", List.of("l-item1"));

	@BeforeEach
	void setUp() {
		client = mock(PluggyClient.class);
		registry = mock(OwnerRegistry.class);
		when(registry.all()).thenReturn(List.of(marcelo, luana));
		when(registry.require("marcelo")).thenReturn(marcelo);
		when(registry.require("luana")).thenReturn(luana);
		when(registry.require("fulano")).thenThrow(new UnknownOwnerException("fulano"));
		service = new LoanService(client, registry, new OwnerFanOut(registry));
		when(client.listLoans(anyString(), anyString())).thenReturn(page());
	}

	@Test
	void returnsOneEntryPerOwner() {
		when(client.listLoans("marcelo", "m-item1")).thenReturn(page(loan("l1")));
		when(client.listLoans("marcelo", "m-item2")).thenReturn(page());
		when(client.listLoans("luana", "l-item1")).thenReturn(page(loan("l2")));

		OwnersResponse<OwnerLoansResult> response = service.getLoans(null);

		assertThat(response.owners()).extracting(OwnerLoansResult::owner).containsExactly("marcelo", "luana");
		assertThat(response.owners().get(0).loans()).extracting("id").containsExactly("l1");
		assertThat(response.owners().get(1).loans()).extracting("id").containsExactly("l2");
		assertThat(response.owners()).allSatisfy(entry -> assertThat(entry.error()).isNull());
	}

	@Test
	void restrictsToTheRequestedOwner() {
		when(client.listLoans("luana", "l-item1")).thenReturn(page(loan("l2")));

		OwnersResponse<OwnerLoansResult> response = service.getLoans("luana");

		assertThat(response.owners()).singleElement().satisfies(entry -> {
			assertThat(entry.owner()).isEqualTo("luana");
			assertThat(entry.loans()).extracting("id").containsExactly("l2");
		});
		verify(client, never()).listLoans(eq("marcelo"), anyString());
	}

	@Test
	void sumsLoansOfAllItemsOfTheSameOwner() {
		when(client.listLoans("marcelo", "m-item1")).thenReturn(page(loan("a"), loan("b")));
		when(client.listLoans("marcelo", "m-item2")).thenReturn(page(loan("c")));

		OwnersResponse<OwnerLoansResult> response = service.getLoans("marcelo");

		assertThat(response.owners().get(0).loans()).extracting("id").containsExactly("a", "b", "c");
	}

	@Test
	void ownerWithoutLoansHasAnEmptyListAndNoError() {
		OwnersResponse<OwnerLoansResult> response = service.getLoans(null);

		assertThat(response.owners()).allSatisfy(entry -> {
			assertThat(entry.loans()).isEmpty();
			assertThat(entry.error()).isNull();
		});
	}

	@Test
	void ownerWithAnEmptyPageBodyHasAnEmptyList() {
		when(client.listLoans("luana", "l-item1")).thenReturn(new PluggyPage<>(1, 0, 0, null));

		assertThat(service.getLoans("luana").owners().get(0).loans()).isEmpty();
	}

	@Test
	void oneOwnerFailingDoesNotDropTheOther() {
		when(client.listLoans("marcelo", "m-item1"))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));
		when(client.listLoans("luana", "l-item1")).thenReturn(page(loan("l2")));

		OwnersResponse<OwnerLoansResult> response = service.getLoans(null);

		OwnerLoansResult failed = response.owners().get(0);
		assertThat(failed.owner()).isEqualTo("marcelo");
		assertThat(failed.loans()).isEmpty();
		assertThat(failed.error()).isNotNull();
		assertThat(failed.error().status()).isEqualTo(502);
		assertThat(response.owners().get(1).loans()).extracting("id").containsExactly("l2");
		assertThat(response.owners().get(1).error()).isNull();
	}

	@Test
	void allOwnersFailingIsAnError() {
		when(client.listLoans(anyString(), anyString()))
				.thenThrow(new PluggyException("x", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		assertThatThrownBy(() -> service.getLoans(null)).isInstanceOf(AllOwnersFailedException.class);
	}

	@Test
	void failureOfASingleRequestedOwnerFollowsTheNormalErrorFlow() {
		when(client.listLoans("luana", "l-item1"))
				.thenThrow(new PluggyException("luana", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		assertThatThrownBy(() -> service.getLoans("luana")).isInstanceOf(PluggyException.class);
	}

	@Test
	void unknownOwnerIsNotFoundWithoutCallingPluggy() {
		assertThatThrownBy(() -> service.getLoans("fulano")).isInstanceOf(UnknownOwnerException.class);

		verify(client, never()).listLoans(anyString(), anyString());
	}

	private static PluggyPage<PluggyLoan> page(PluggyLoan... loans) {
		return new PluggyPage<>(1, loans.length, 1, List.of(loans));
	}

	private static PluggyLoan loan(String id) {
		return new PluggyLoan(id, "item", null, "Empréstimo " + id, null, "LOAN", new BigDecimal("1000"), "BRL", null,
				null, null, null);
	}

}
