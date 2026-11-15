package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.resource.dto.IncomesSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

class IncomesSummaryServiceTest {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 15);

	private TransactionService transactionService;

	private IncomesSummaryService service;

	@BeforeEach
	void setUp() {
		transactionService = mock(TransactionService.class);
		service = new IncomesSummaryService(transactionService);
	}

	@Test
	void sumsBalancesAndCreditsOfBankAccountsInBrl() {
		txs("marcelo", "m1", tx("t1", "PIX Recebido", "19.99", "BRL", TransactionTypesEnum.CREDIT));
		txs("marcelo", "m2");
		txs("luana", "l1", tx("t2", "Salário", "1000", "BRL", TransactionTypesEnum.CREDIT));

		SectionResult<IncomesSummary> result = service.summarize(
				List.of(owner("marcelo", bank("m1", "Nubank", "500.50"), bank("m2", "Itaú", "100")),
						owner("luana", bank("l1", "Inter", "200"))),
				FROM, TO);

		IncomesSummary incomes = result.section();
		assertThat(result.errors()).isEmpty();
		assertThat(incomes.available().total().value()).isEqualByComparingTo("800.50");
		assertThat(incomes.available().total().currencyCode()).isEqualTo("BRL");
		assertThat(incomes.available().detail()).containsOnlyKeys("marcelo", "luana");
		assertThat(incomes.available().detail().get("marcelo")).hasSize(2);
		assertThat(incomes.transactions().total().value()).isEqualByComparingTo("1019.99");
		assertThat(incomes.transactions().detail().get("marcelo")).singleElement()
				.satisfies(t -> assertThat(t.accountId()).isEqualTo("m1"));
	}

	@Test
	void ignoresNonBankAccountsAndNonBrlAccounts() {
		txs("marcelo", "m1");

		IncomesSummary incomes = service.summarize(List.of(owner("marcelo", bank("m1", "Nubank", "500"),
				account("c1", "Cartão", AccountTypesEnum.CREDIT, "999", "BRL"),
				account("u1", "Dólar", AccountTypesEnum.BANK, "300", "USD"))), FROM, TO).section();

		assertThat(incomes.available().detail().get("marcelo")).extracting("id").containsExactly("m1");
		assertThat(incomes.available().total().value()).isEqualByComparingTo("500");
	}

	@Test
	void keepsOnlyBrlCreditTransactions() {
		txs("marcelo", "m1", tx("in", "PIX", "50", "BRL", TransactionTypesEnum.CREDIT),
				tx("out", "Mercado", "30", "BRL", TransactionTypesEnum.DEBIT),
				tx("usd", "Exterior", "10", "USD", TransactionTypesEnum.CREDIT));

		IncomesSummary incomes = service.summarize(List.of(owner("marcelo", bank("m1", "Nubank", "0"))), FROM, TO)
				.section();

		assertThat(incomes.transactions().detail().get("marcelo")).extracting("id").containsExactly("in");
		assertThat(incomes.transactions().total().value()).isEqualByComparingTo("50");
	}

	@Test
	void keepsTransferBetweenOwnersAsIncome() {
		txs("marcelo", "m1");
		txs("luana", "l1", tx("t1", "PIX Recebido de Marcelo", "300", "BRL", TransactionTypesEnum.CREDIT));

		IncomesSummary incomes = service.summarize(List.of(owner("marcelo", bank("m1", "Nubank", "0")),
				owner("luana", bank("l1", "Inter", "0"))), FROM, TO).section();

		assertThat(incomes.transactions().detail().get("luana")).extracting("id").containsExactly("t1");
	}

	@Test
	void dropsOwnerFromTransactionsOnlyWhenTransactionsFail() {
		when(transactionService.getAllTransactions(eq("marcelo"), eq("m1"), any(), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.INTERNAL_SERVER_ERROR, "falhou"));
		txs("luana", "l1", tx("t1", "PIX", "20", "BRL", TransactionTypesEnum.CREDIT));

		SectionResult<IncomesSummary> result = service.summarize(
				List.of(owner("marcelo", bank("m1", "Nubank", "500")), owner("luana", bank("l1", "Inter", "200"))),
				FROM, TO);

		IncomesSummary incomes = result.section();
		assertThat(result.errors()).singleElement().satisfies(e -> assertThat(e.owner()).isEqualTo("marcelo"));
		assertThat(incomes.available().detail()).containsOnlyKeys("marcelo", "luana");
		assertThat(incomes.transactions().detail()).containsOnlyKeys("luana");
		assertThat(incomes.available().total().value()).isEqualByComparingTo("700");
		assertThat(incomes.transactions().total().value()).isEqualByComparingTo("20");
	}

	private void txs(String owner, String accountId, Transaction... txs) {
		when(transactionService.getAllTransactions(owner, accountId, FROM, TO)).thenReturn(List.of(txs));
	}

	private static OwnerAccountsResult owner(String name, Account... accounts) {
		return new OwnerAccountsResult(name, List.of(accounts), null);
	}

	private static Account bank(String id, String name, String balance) {
		return account(id, name, AccountTypesEnum.BANK, balance, "BRL");
	}

	private static Account account(String id, String name, AccountTypesEnum type, String balance, String currency) {
		return new Account(id, "item", type, null, name, null, new BigDecimal(balance), currency, null);
	}

	private static Transaction tx(String id, String description, String amount, String currency,
			TransactionTypesEnum type) {
		return new Transaction(id, Instant.parse("2026-11-10T12:00:00Z").atZone(ZONE).toOffsetDateTime(),
				description, new BigDecimal(amount), currency, type, "POSTED", null, null, null, null);
	}

}
