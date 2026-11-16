package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.AccountFailure;
import com.mfaraujo.financialapi.domain.result.OwnedTransaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.TransactionListResult;
import com.mfaraujo.financialapi.exception.AllAccountsFailedException;
import com.mfaraujo.financialapi.exception.InvalidRequestException;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class TransactionListServiceTest {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

	private static final LocalDate MONTH_START = LocalDate.of(2026, 11, 1);

	private AccountService accountService;

	private TransactionService transactionService;

	private TransactionListService service;

	@BeforeEach
	void setUp() {
		accountService = mock(AccountService.class);
		transactionService = mock(TransactionService.class);
		Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
		service = new TransactionListService(accountService, transactionService, clock);
		when(transactionService.getAllTransactions(anyString(), anyString(), any(), any())).thenReturn(List.of());
	}

	// ---- quais donos e contas

	@Test
	void queriesEveryAccountOfEveryOwnerAndTagsTheOrigin() {
		accounts(owner("marcelo", account("m1"), account("m2")), owner("luana", account("l1")));
		txs("marcelo", "m1", tx("a", "2026-11-10"));
		txs("marcelo", "m2", tx("b", "2026-11-09"));
		txs("luana", "l1", tx("c", "2026-11-08"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(result.failures()).isEmpty();
		assertThat(result.transactions()).extracting(OwnedTransaction::owner, OwnedTransaction::accountId,
				t -> t.transaction().id()).containsExactly(
						tuple("marcelo", "m1", "a"),
						tuple("marcelo", "m2", "b"),
						tuple("luana", "l1", "c"));
		verify(accountService).getAccounts(null);
	}

	@Test
	void queriesOnlyTheRequestedOwner() {
		when(accountService.getAccounts("luana")).thenReturn(new OwnersResponse<>(List.of(
				owner("luana", account("l1")))));
		txs("luana", "l1", tx("c", "2026-11-08"));

		TransactionListResult result = service.list("luana", null, null, null);

		assertThat(result.transactions()).extracting(OwnedTransaction::owner).containsExactly("luana");
		verify(transactionService, never()).getAllTransactions(eq("marcelo"),
				anyString(), any(), any());
	}

	@Test
	void queriesOnlyTheRequestedAccountWithoutListingAccounts() {
		txs("marcelo", "m2", tx("b", "2026-11-09"));

		TransactionListResult result = service.list("marcelo", "m2", null, null);

		assertThat(result.transactions()).singleElement().satisfies(t -> {
			assertThat(t.owner()).isEqualTo("marcelo");
			assertThat(t.accountId()).isEqualTo("m2");
			assertThat(t.transaction().id()).isEqualTo("b");
		});
		assertThat(result.failures()).isEmpty();
		verifyNoInteractions(accountService);
	}

	@Test
	void accountWithoutOwnerIsABadRequestAndDoesNotQueryAnything() {
		assertThatThrownBy(() -> service.list(null, "m1", null, null)).isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("owner");

		verifyNoInteractions(accountService, transactionService);
	}

	@Test
	void unknownOwnerPropagatesAsNotFound() {
		when(accountService.getAccounts("fulano")).thenThrow(new UnknownOwnerException("fulano"));
		when(transactionService.getAllTransactions(eq("fulano"), anyString(), any(),
				any())).thenThrow(new UnknownOwnerException("fulano"));

		assertThatThrownBy(() -> service.list("fulano", null, null, null)).isInstanceOf(UnknownOwnerException.class);
		assertThatThrownBy(() -> service.list("fulano", "x1", null, null)).isInstanceOf(UnknownOwnerException.class);
	}

	@Test
	void accountOfAnotherOwnerPropagatesAsTheNormalError() {
		when(transactionService.getAllTransactions("marcelo", "l1", MONTH_START, TODAY))
				.thenThrow(new PluggyException("marcelo", HttpStatus.NOT_FOUND, "conta não encontrada"));

		assertThatThrownBy(() -> service.list("marcelo", "l1", null, null)).isInstanceOf(PluggyException.class);
	}

	// ---- período

	@Test
	void defaultsToTheCurrentMonthUntilToday() {
		accounts(owner("marcelo", account("m1")));

		service.list(null, null, null, null);

		verify(transactionService).getAllTransactions("marcelo", "m1", MONTH_START, TODAY);
	}

	@Test
	void usesDateFromUntilToday() {
		accounts(owner("marcelo", account("m1")));

		service.list(null, null, LocalDate.of(2026, 10, 10), null);

		verify(transactionService).getAllTransactions("marcelo", "m1", LocalDate.of(2026, 10, 10), TODAY);
	}

	@Test
	void usesTheFirstDayOfTheDateToMonthUntilDateTo() {
		accounts(owner("marcelo", account("m1")));

		service.list(null, null, null, LocalDate.of(2026, 9, 20));

		verify(transactionService).getAllTransactions("marcelo", "m1", LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 20));
	}

	@Test
	void usesBothDatesWhenGiven() {
		txs("marcelo", "m1");

		service.list("marcelo", "m1", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 20));

		verify(transactionService).getAllTransactions("marcelo", "m1", LocalDate.of(2026, 8, 5),
				LocalDate.of(2026, 9, 20));
	}

	// ---- ordem

	@Test
	void ordersByDateFromTheMostRecentAcrossOwnersAndAccounts() {
		accounts(owner("marcelo", account("m1"), account("m2")), owner("luana", account("l1")));
		txs("marcelo", "m1", tx("m1-old", "2026-11-02"));
		txs("marcelo", "m2", tx("m2-new", "2026-11-12"));
		txs("luana", "l1", tx("l1-mid", "2026-11-07"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("m2-new", "l1-mid", "m1-old");
	}

	@Test
	void breaksDateTiesByOwnerThenByAccount() {
		accounts(owner("marcelo", account("m2"), account("m1")), owner("luana", account("l9")));
		txs("marcelo", "m2", tx("marcelo-m2", "2026-11-10"));
		txs("marcelo", "m1", tx("marcelo-m1", "2026-11-10"));
		txs("luana", "l9", tx("luana-l9", "2026-11-10"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("luana-l9", "marcelo-m1", "marcelo-m2");
	}

	@Test
	void keepsThePluggyOrderInsideTheSameAccountAndDate() {
		accounts(owner("marcelo", account("m1")));
		txs("marcelo", "m1", tx("z-first", "2026-11-10"), tx("a-second", "2026-11-10"), tx("m-third", "2026-11-10"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("z-first", "a-second", "m-third");
	}

	@Test
	void putsTransactionsWithoutDateLast() {
		accounts(owner("marcelo", account("m1")));
		txs("marcelo", "m1", new Transaction("sem-data", null, "x", BigDecimal.ONE, "BRL", TransactionTypesEnum.DEBIT,
				"POSTED", null, null, null, null), tx("com-data", "2026-11-10"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("com-data", "sem-data");
	}

	// ---- falhas

	@Test
	void oneFailingAccountKeepsTheOthersAndIsRecorded() {
		accounts(owner("marcelo", account("m1"), account("m2")));
		when(transactionService.getAllTransactions("marcelo", "m1", MONTH_START, TODAY))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));
		txs("marcelo", "m2", tx("b", "2026-11-09"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("b");
		assertThat(result.failures()).singleElement().satisfies(failure -> {
			assertThat(failure.owner()).isEqualTo("marcelo");
			assertThat(failure.accountId()).isEqualTo("m1");
			assertThat(failure.error().status()).isEqualTo(502);
		});
	}

	@Test
	void oneFailingAccountIsRecordedEvenWhenTheOwnerIsRequested() {
		when(accountService.getAccounts("marcelo")).thenReturn(new OwnersResponse<>(List.of(
				owner("marcelo", account("m1"), account("m2")))));
		when(transactionService.getAllTransactions("marcelo", "m1", MONTH_START, TODAY))
				.thenThrow(new PluggyException("marcelo", HttpStatus.TOO_MANY_REQUESTS, "limite"));
		txs("marcelo", "m2", tx("b", "2026-11-09"));

		TransactionListResult result = service.list("marcelo", null, null, null);

		assertThat(ids(result)).containsExactly("b");
		assertThat(result.failures()).extracting(AccountFailure::accountId).containsExactly("m1");
		assertThat(result.failures().get(0).error().status()).isEqualTo(429);
	}

	@Test
	void anOwnerWhoseAccountListingFailedIsRecordedWithoutAccount() {
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");
		accounts(new OwnerAccountsResult("luana", List.of(), error), owner("marcelo", account("m1")));
		txs("marcelo", "m1", tx("a", "2026-11-10"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("a");
		assertThat(result.failures()).singleElement().satisfies(failure -> {
			assertThat(failure.owner()).isEqualTo("luana");
			assertThat(failure.accountId()).isNull();
			assertThat(failure.error()).isEqualTo(error);
		});
	}

	@Test
	void anOwnerWithoutAccountsIsASuccessWithNoFailures() {
		accounts(owner("marcelo"), owner("luana", account("l1")));
		txs("luana", "l1", tx("c", "2026-11-08"));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(ids(result)).containsExactly("c");
		assertThat(result.failures()).isEmpty();
	}

	@Test
	void anOwnerWithoutAccountsKeepsTheResponseAliveWhenTheOtherFails() {
		ApiError error = new ApiError(502, "Bad Gateway", "falhou");
		accounts(owner("marcelo"), new OwnerAccountsResult("luana", List.of(), error));

		TransactionListResult result = service.list(null, null, null, null);

		assertThat(result.transactions()).isEmpty();
		assertThat(result.failures()).extracting(AccountFailure::owner).containsExactly("luana");
	}

	@Test
	void failsWithBadGatewayWhenNoQuerySucceeds() {
		accounts(owner("marcelo", account("m1"), account("m2")));
		when(transactionService.getAllTransactions(anyString(), anyString(), any(), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));

		assertThatThrownBy(() -> service.list(null, null, null, null)).isInstanceOf(AllAccountsFailedException.class);
	}

	@Test
	void everythingSucceedingHasNoFailures() {
		accounts(owner("marcelo", account("m1")));
		txs("marcelo", "m1", tx("a", "2026-11-10"));

		assertThat(service.list(null, null, null, null).failures()).isEmpty();
	}

	// ---- helpers

	private void accounts(OwnerAccountsResult... owners) {
		when(accountService.getAccounts(null)).thenReturn(new OwnersResponse<>(List.of(owners)));
	}

	private void txs(String owner, String accountId, Transaction... txs) {
		when(transactionService.getAllTransactions(owner, accountId, MONTH_START, TODAY)).thenReturn(List.of(txs));
	}

	private static OwnerAccountsResult owner(String name, Account... accounts) {
		return new OwnerAccountsResult(name, List.of(accounts), null);
	}

	private static Account account(String id) {
		return new Account(id, "item", AccountTypesEnum.BANK, null, "Conta " + id, null, BigDecimal.TEN, "BRL", null);
	}

	private static Transaction tx(String id, String date) {
		return new Transaction(id, OffsetDateTime.parse(date + "T00:00:00Z"), "Descrição " + id, BigDecimal.TEN, "BRL",
				TransactionTypesEnum.DEBIT, "POSTED", null, null, null, null);
	}

	private static List<String> ids(TransactionListResult result) {
		return result.transactions().stream().map(t -> t.transaction().id()).toList();
	}

}
