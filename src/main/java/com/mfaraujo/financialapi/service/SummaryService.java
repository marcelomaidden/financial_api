package com.mfaraujo.financialapi.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.TransactionPage;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.IncomeAccount;
import com.mfaraujo.financialapi.resource.dto.IncomeTransaction;
import com.mfaraujo.financialapi.resource.dto.IncomesSummary;
import com.mfaraujo.financialapi.resource.dto.Money;
import com.mfaraujo.financialapi.resource.dto.SummaryError;
import com.mfaraujo.financialapi.resource.dto.SummaryResponse;
import com.mfaraujo.financialapi.resource.dto.SummarySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static com.mfaraujo.financialapi.domain.enums.CurrencyEnum.BRL;

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
		LocalDate today = dateTo == null ? LocalDate.now(clock) : dateTo;
		LocalDate monthStart = dateFrom == null ? today.withDayOfMonth(1) : dateFrom;

		Map<String, List<IncomeAccount>> availableDetail = new LinkedHashMap<>();
		Map<String, List<IncomeTransaction>> transactionsDetail = new LinkedHashMap<>();
		List<SummaryError> errors = new ArrayList<>();

		for (OwnerAccountsResult owner : accountService.getAccounts(null).owners()) {
			if (owner.error() != null) {
				errors.add(toError(owner.owner(), owner.error()));
				continue;
			}
			List<Account> bankAccounts = bankAccounts(owner);
			availableDetail.put(owner.owner(), toIncomeAccounts(bankAccounts));

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

		return new SummaryResponse(incomesSummary(availableDetail, transactionsDetail),
				                   !errors.isEmpty(), errors);
	}

	private List<Account> bankAccounts(OwnerAccountsResult owner) {
		return owner.accounts().stream()
				.filter(Account::isBrlBankAccount)
				.toList();
	}

	private List<IncomeAccount> toIncomeAccounts(List<Account> bankAccounts) {
		return bankAccounts.stream()
				.map(a -> new IncomeAccount(a.id(), a.name(), a.balance(), a.currencyCode()))
				.toList();
	}

	/** Monta o resumo de entradas e transações do período de todas as contas do dono. */
	private IncomesSummary incomesSummary(Map<String, List<IncomeAccount>> availableDetail,
			Map<String, List<IncomeTransaction>> transactionsDetail) {
		Money totalBalance = brl(availableDetail.values().stream().flatMap(List::stream)
				.map(IncomeAccount::balance).toList());

		Money totalTransactions = brl(transactionsDetail.values().stream().flatMap(List::stream)
				.map(IncomeTransaction::amount).toList());

		return new IncomesSummary(
				new SummarySection<>(totalBalance, availableDetail),
				new SummarySection<>(totalTransactions, transactionsDetail));
	}

	/** Entradas do período de todas as contas do dono. Qualquer falha descarta o dono inteiro. */
	private List<IncomeTransaction> incomesOf(String owner, List<Account> accounts, LocalDate from,
			LocalDate to) {
		List<IncomeTransaction> incomes = new ArrayList<>();
		for (Account account : accounts) {
			String after = null;
			do {
				TransactionPage page = transactionService.getTransactions(owner, account.id(), from, to, after);
				for (Transaction tx : page.transactions()) {
					if (tx.isBrlCredit()) {
						incomes.add(buildIncomeTransaction(tx, account.id()));
					}
				}
				after = page.nextCursor();
			}
			while (after != null);
		}
		return incomes;
	}

	private IncomeTransaction buildIncomeTransaction(Transaction tx, String accountId) {
		return new IncomeTransaction(tx.id(), tx.description(), tx.amount(),
				                    tx.currencyCode(), tx.date(), accountId);
	}

	private static Money brl(List<BigDecimal> values) {
		return new Money(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add), BRL.getType());
	}

	private static SummaryError toError(String owner, ApiError error) {
		return new SummaryError(owner, error.status(), error.message());
	}

}
