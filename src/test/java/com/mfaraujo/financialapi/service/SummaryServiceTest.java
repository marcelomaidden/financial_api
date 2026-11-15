package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.mfaraujo.financialapi.exception.AllOwnersFailedException;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.SummaryResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;
import com.mfaraujo.financialapi.service.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.service.enums.TransactionTypesEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class SummaryServiceTest {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

	private static final LocalDate MONTH_START = LocalDate.of(2026, 11, 1);

	private AccountService accountService;

	private TransactionService transactionService;

	private SummaryService service;

	@BeforeEach
	void setUp() {
		accountService = mock(AccountService.class);
		transactionService = mock(TransactionService.class);
		Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
		service = new SummaryService(accountService, transactionService, clock);
	}

	@Test
	void sumsBalancesAndCreditsOfBankAccountsInBrl() {
		accounts(owner("marcelo", bank("m1", "Nubank", "500.50"), bank("m2", "Itaú", "100")),
				owner("luana", bank("l1", "Inter", "200")));
		page("marcelo", "m1", tx("t1", "PIX Recebido", "19.99", "BRL", TransactionTypesEnum.CREDIT));
		page("marcelo", "m2");
		page("luana", "l1", tx("t2", "Salário", "1000", "BRL", TransactionTypesEnum.CREDIT));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isFalse();
		assertThat(summary.errors()).isEmpty();
		assertThat(summary.incomes().available().total().value()).isEqualByComparingTo("800.50");
		assertThat(summary.incomes().available().total().currencyCode()).isEqualTo("BRL");
		assertThat(summary.incomes().available().detail()).containsOnlyKeys("marcelo", "luana");
		assertThat(summary.incomes().available().detail().get("marcelo")).hasSize(2);
		assertThat(summary.incomes().transactions().total().value()).isEqualByComparingTo("1019.99");
		assertThat(summary.incomes().transactions().detail().get("marcelo")).singleElement()
				.satisfies(t -> assertThat(t.accountId()).isEqualTo("m1"));
	}

	@Test
	void ignoresNonBankAccountsAndNonBrlAccounts() {
		accounts(owner("marcelo", bank("m1", "Nubank", "500"), account("c1", "Cartão", AccountTypesEnum.CREDIT, "999", "BRL"),
				account("u1", "Dólar", AccountTypesEnum.BANK, "300", "USD")));
		page("marcelo", "m1");

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.incomes().available().detail().get("marcelo")).extracting("id").containsExactly("m1");
		assertThat(summary.incomes().available().total().value()).isEqualByComparingTo("500");
	}

	@Test
	void keepsOnlyBrlCreditTransactions() {
		accounts(owner("marcelo", bank("m1", "Nubank", "0")));
		page("marcelo", "m1", tx("in", "PIX", "50", "BRL", TransactionTypesEnum.CREDIT), tx("out", "Mercado", "30", "BRL", TransactionTypesEnum.DEBIT),
				tx("usd", "Exterior", "10", "USD", TransactionTypesEnum.CREDIT));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.incomes().transactions().detail().get("marcelo")).extracting("id").containsExactly("in");
		assertThat(summary.incomes().transactions().total().value()).isEqualByComparingTo("50");
	}

	@Test
	void queriesTransactionsFromMonthStartAndFollowsPages() {
		accounts(owner("marcelo", bank("m1", "Nubank", "0")));
		when(transactionService.getTransactions("marcelo", "m1", MONTH_START, TODAY, null))
				.thenReturn(new TransactionsResponse("marcelo", "m1",
						List.of(tx("t1", "A", "10", "BRL", TransactionTypesEnum.CREDIT)), "cursor-2"));
		when(transactionService.getTransactions("marcelo", "m1", MONTH_START, TODAY, "cursor-2"))
				.thenReturn(new TransactionsResponse("marcelo", "m1",
						List.of(tx("t2", "B", "5", "BRL", TransactionTypesEnum.CREDIT)), null));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.incomes().transactions().detail().get("marcelo")).extracting("id")
				.containsExactly("t1", "t2");
		assertThat(summary.incomes().transactions().total().value()).isEqualByComparingTo("15");
	}

	@Test
	void keepsTransferBetweenOwnersAsIncome() {
		accounts(owner("marcelo", bank("m1", "Nubank", "0")), owner("luana", bank("l1", "Inter", "0")));
		page("marcelo", "m1");
		page("luana", "l1", tx("t1", "PIX Recebido de Marcelo", "300", "BRL", TransactionTypesEnum.CREDIT));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.incomes().transactions().detail().get("luana")).extracting("id").containsExactly("t1");
	}

	@Test
	void dropsOwnerWhoseAccountsFailAndKeepsTheOther() {
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");
		accounts(new OwnerAccounts("luana", List.of(), error), owner("marcelo", bank("m1", "Nubank", "500")));
		page("marcelo", "m1", tx("t1", "PIX", "20", "BRL", TransactionTypesEnum.CREDIT));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isTrue();
		assertThat(summary.errors()).singleElement().satisfies(e -> {
			assertThat(e.owner()).isEqualTo("luana");
			assertThat(e.status()).isEqualTo(502);
		});
		assertThat(summary.incomes().available().detail()).containsOnlyKeys("marcelo");
		assertThat(summary.incomes().transactions().detail()).containsOnlyKeys("marcelo");
		assertThat(summary.incomes().available().total().value()).isEqualByComparingTo("500");
	}

	@Test
	void dropsOwnerFromTransactionsOnlyWhenTransactionsFail() {
		accounts(owner("marcelo", bank("m1", "Nubank", "500")), owner("luana", bank("l1", "Inter", "200")));
		when(transactionService.getTransactions(eq("marcelo"), eq("m1"), any(), any(), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));
		page("luana", "l1", tx("t1", "PIX", "20", "BRL", TransactionTypesEnum.CREDIT));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isTrue();
		assertThat(summary.errors()).singleElement().satisfies(e -> assertThat(e.owner()).isEqualTo("marcelo"));
		assertThat(summary.incomes().available().detail()).containsOnlyKeys("marcelo", "luana");
		assertThat(summary.incomes().transactions().detail()).containsOnlyKeys("luana");
		assertThat(summary.incomes().available().total().value()).isEqualByComparingTo("700");
		assertThat(summary.incomes().transactions().total().value()).isEqualByComparingTo("20");
	}

	@Test
	void propagatesFailureWhenAllOwnersFail() {
		when(accountService.getAccounts(null)).thenThrow(new AllOwnersFailedException());

		assertThatThrownBy(() -> service.getSummary(null, null)).isInstanceOf(AllOwnersFailedException.class);
	}

	private void accounts(OwnerAccounts... owners) {
		when(accountService.getAccounts(null)).thenReturn(new OwnersResponse<>(List.of(owners)));
	}

	private void page(String owner, String accountId, TransactionResponse... txs) {
		when(transactionService.getTransactions(eq(owner), eq(accountId), eq(MONTH_START), eq(TODAY), eq(null)))
				.thenReturn(new TransactionsResponse(owner, accountId, List.of(txs), null));
	}

	private static OwnerAccounts owner(String name, AccountResponse... accounts) {
		return new OwnerAccounts(name, List.of(accounts), null);
	}

	private static AccountResponse bank(String id, String name, String balance) {
		return account(id, name, AccountTypesEnum.BANK, balance, "BRL");
	}

	private static AccountResponse account(String id, String name, AccountTypesEnum type, String balance, String currency) {
		return new AccountResponse(id, "item", type, null, name, null, new BigDecimal(balance), currency);
	}

	private static TransactionResponse tx(String id, String description, String amount, String currency,
										  TransactionTypesEnum type) {
		return new TransactionResponse(id, Instant.parse("2026-11-10T12:00:00Z").atZone(ZONE).toOffsetDateTime(),
				description, new BigDecimal(amount), currency, type, "POSTED", null);
	}

}
