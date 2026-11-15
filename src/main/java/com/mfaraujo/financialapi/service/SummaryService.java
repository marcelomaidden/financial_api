package com.mfaraujo.financialapi.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.IncomeAccount;
import com.mfaraujo.financialapi.resource.dto.IncomeTransaction;
import com.mfaraujo.financialapi.resource.dto.IncomesSummary;
import com.mfaraujo.financialapi.resource.dto.Money;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.SummaryError;
import com.mfaraujo.financialapi.resource.dto.SummaryResponse;
import com.mfaraujo.financialapi.resource.dto.SummarySection;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

/**
 * Resumo de {@code incomes}: saldo das contas {@code BANK} e entradas
 * ({@code CREDIT}) do mês corrente nessas contas, só em BRL.
 * <p>
 * A falha de um dono tira o dono da parte afetada e entra em {@code errors}. Se
 * a consulta de contas falhar para todos, {@code AccountService} lança
 * {@code AllOwnersFailedException} (502).
 */
@Service
public class SummaryService {

	static final String BANK = "BANK";

	static final String CREDIT = "CREDIT";

	static final String BRL = "BRL";

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

	private final AccountService accountService;

	private final TransactionService transactionService;

	private final Clock clock;

	@Autowired
	public SummaryService(AccountService accountService, TransactionService transactionService) {
		this(accountService, transactionService, Clock.system(ZONE));
	}

	SummaryService(AccountService accountService, TransactionService transactionService, Clock clock) {
		this.accountService = accountService;
		this.transactionService = transactionService;
		this.clock = clock;
	}

	public SummaryResponse getSummary(LocalDate dateFrom, LocalDate dateTo) {
		LocalDate today = dateFrom == null ? LocalDate.now(clock) : dateFrom;
		LocalDate monthStart = dateTo == null ? today.withDayOfMonth(1) : dateTo;

		Map<String, List<IncomeAccount>> availableDetail = new LinkedHashMap<>();
		Map<String, List<IncomeTransaction>> transactionsDetail = new LinkedHashMap<>();
		List<SummaryError> errors = new ArrayList<>();

		for (OwnerAccounts owner : accountService.getAccounts(null).owners()) {
			if (owner.error() != null) {
				errors.add(toError(owner.owner(), owner.error()));
				continue;
			}
			List<AccountResponse> bankAccounts = owner.accounts().stream()
					.filter(a -> BANK.equals(a.type()) && BRL.equals(a.currencyCode()) && a.balance() != null)
					.toList();
			availableDetail.put(owner.owner(), bankAccounts.stream()
					.map(a -> new IncomeAccount(a.id(), a.name(), a.balance(), a.currencyCode()))
					.toList());
			try {
				transactionsDetail.put(owner.owner(), incomesOf(owner.owner(), bankAccounts, monthStart, today));
			}
			catch (RuntimeException ex) {
				ApiError error = ApiErrors.from(ex);
				log.warn("Consulta de transações falhou para o dono '{}': {} {}", owner.owner(), error.status(),
						error.message());
				log.debug("Detalhe da falha de transações do dono '{}'", owner.owner(), ex);
				errors.add(toError(owner.owner(), error));
			}
		}

		IncomesSummary incomes = new IncomesSummary(
				new SummarySection<>(brl(availableDetail.values().stream().flatMap(List::stream)
						.map(IncomeAccount::balance).toList()), availableDetail),
				new SummarySection<>(brl(transactionsDetail.values().stream().flatMap(List::stream)
						.map(IncomeTransaction::amount).toList()), transactionsDetail));
		return new SummaryResponse(incomes, !errors.isEmpty(), errors);
	}

	/** Entradas do período de todas as contas do dono. Qualquer falha descarta o dono inteiro. */
	private List<IncomeTransaction> incomesOf(String owner, List<AccountResponse> accounts, LocalDate from,
			LocalDate to) {
		List<IncomeTransaction> incomes = new ArrayList<>();
		for (AccountResponse account : accounts) {
			String after = null;
			do {
				TransactionsResponse page = transactionService.getTransactions(owner, account.id(), from, to, after);
				for (TransactionResponse tx : page.transactions()) {
					if (CREDIT.equals(tx.type()) && BRL.equals(tx.currencyCode()) && tx.amount() != null) {
						incomes.add(new IncomeTransaction(tx.id(), tx.description(), tx.amount(), tx.currencyCode(),
								account.id()));
					}
				}
				after = page.nextCursor();
			}
			while (after != null);
		}
		return incomes;
	}

	private static Money brl(List<BigDecimal> values) {
		return new Money(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add), BRL);
	}

	private static SummaryError toError(String owner, ApiError error) {
		return new SummaryError(owner, error.status(), error.message());
	}

}
