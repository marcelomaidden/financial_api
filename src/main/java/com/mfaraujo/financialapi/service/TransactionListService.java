package com.mfaraujo.financialapi.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.AccountFailure;
import com.mfaraujo.financialapi.domain.result.OwnedTransaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.TransactionListResult;
import com.mfaraujo.financialapi.exception.AllAccountsFailedException;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.exception.InvalidRequestException;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Transações de vários donos e contas em uma lista só, para {@code GET /transactions}.
 * <p>
 * Com {@code accountId} (que exige {@code owner}), consulta só essa conta e qualquer falha
 * segue o fluxo normal de erro. Sem {@code accountId}, consulta todas as contas do dono, ou
 * de todos os donos, e a falha de uma conta, ou de um dono ao listar as contas, vira
 * {@link AccountFailure} sem derrubar o resto. Se nenhuma consulta tiver sucesso e houver
 * falha, lança {@link AllAccountsFailedException}.
 */
@Service
public class TransactionListService {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final Logger log = LoggerFactory.getLogger(TransactionListService.class);

	/**
	 * Data decrescente (sem data por último), depois dono e conta. A ordenação é estável,
	 * então a ordem da Pluggy se mantém dentro da mesma conta e data.
	 */
	private static final Comparator<OwnedTransaction> ORDER = Comparator
			.comparing((OwnedTransaction t) -> t.transaction().date(),
					Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(OwnedTransaction::owner)
			.thenComparing(OwnedTransaction::accountId);

	private final AccountService accountService;

	private final TransactionService transactionService;

	private final Clock clock;

	@Autowired
	public TransactionListService(AccountService accountService, TransactionService transactionService) {
		this(accountService, transactionService, Clock.system(ZONE));
	}

	TransactionListService(AccountService accountService, TransactionService transactionService, Clock clock) {
		this.accountService = accountService;
		this.transactionService = transactionService;
		this.clock = clock;
	}

	public TransactionListResult list(String owner, String accountId, LocalDate dateFrom, LocalDate dateTo) {
		if (accountId != null && owner == null) {
			throw new InvalidRequestException("Informe 'owner' junto com 'accountId'");
		}
		LocalDate to = dateTo == null ? LocalDate.now(clock) : dateTo;
		LocalDate from = dateFrom == null ? to.withDayOfMonth(1) : dateFrom;

		List<OwnedTransaction> transactions = new ArrayList<>();
		if (accountId != null) {
			add(transactions, owner, accountId, transactionService.getAllTransactions(owner, accountId, from, to));
			return new TransactionListResult(sorted(transactions), List.of());
		}

		List<AccountFailure> failures = new ArrayList<>();
		int successes = 0;
		for (OwnerAccountsResult entry : accountService.getAccounts(owner).owners()) {
			if (entry.error() != null) {
				failures.add(new AccountFailure(entry.owner(), null, entry.error()));
				continue;
			}
			if (entry.accounts().isEmpty()) {
				successes++;
			}
			for (Account account : entry.accounts()) {
				try {
					add(transactions, entry.owner(), account.id(),
							transactionService.getAllTransactions(entry.owner(), account.id(), from, to));
					successes++;
				}
				catch (RuntimeException ex) {
					failures.add(new AccountFailure(entry.owner(), account.id(), failureOf(entry.owner(), account, ex)));
				}
			}
		}
		if (successes == 0 && !failures.isEmpty()) {
			throw new AllAccountsFailedException();
		}
		return new TransactionListResult(sorted(transactions), failures);
	}

	private static void add(List<OwnedTransaction> into, String owner, String accountId, List<Transaction> source) {
		source.forEach(tx -> into.add(new OwnedTransaction(owner, accountId, tx)));
	}

	private static List<OwnedTransaction> sorted(List<OwnedTransaction> transactions) {
		List<OwnedTransaction> copy = new ArrayList<>(transactions);
		copy.sort(ORDER);
		return copy;
	}

	private static ApiError failureOf(String owner, Account account, RuntimeException ex) {
		ApiError error = ApiErrors.from(ex);
		log.warn("Consulta de transações falhou para o dono '{}', conta '{}': {} {}", owner, account.id(),
				error.status(), error.message());
		log.debug("Detalhe da falha de transações do dono '{}', conta '{}'", owner, account.id(), ex);
		return error;
	}

}
