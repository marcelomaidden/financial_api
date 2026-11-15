package com.mfaraujo.financialapi.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.IncomeAccount;
import com.mfaraujo.financialapi.resource.dto.IncomeTransaction;
import com.mfaraujo.financialapi.resource.dto.IncomesSummary;
import com.mfaraujo.financialapi.resource.dto.SummaryError;
import com.mfaraujo.financialapi.resource.dto.SummarySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Seção {@code incomes} do resumo: saldo das contas {@code BANK} e entradas
 * ({@code CREDIT}) do período nessas contas, só em BRL.
 * <p>
 * Recebe apenas donos cuja consulta de contas teve sucesso. Uma falha na consulta
 * de transações tira o dono de {@code transactions} e entra nos erros da seção.
 */
@Service
public class IncomesSummaryService {

	private static final Logger log = LoggerFactory.getLogger(IncomesSummaryService.class);

	private final TransactionService transactionService;

	public IncomesSummaryService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	public SectionResult<IncomesSummary> summarize(List<OwnerAccountsResult> owners, LocalDate from, LocalDate to) {
		Map<String, List<IncomeAccount>> availableDetail = new LinkedHashMap<>();
		Map<String, List<IncomeTransaction>> transactionsDetail = new LinkedHashMap<>();
		List<SummaryError> errors = new ArrayList<>();

		for (OwnerAccountsResult owner : owners) {
			List<Account> bankAccounts = bankAccounts(owner);
			availableDetail.put(owner.owner(), toIncomeAccounts(bankAccounts));

			try {
				transactionsDetail.put(owner.owner(), incomesOf(owner.owner(), bankAccounts, from, to));
			}
			catch (RuntimeException ex) {
				ApiError error = ApiErrors.from(ex);
				log.warn("Consulta de transações falhou para o dono '{}': {} {}", owner.owner(), error.status(),
						error.message());
				log.debug("Detalhe da falha de transações do dono '{}'", owner.owner(), ex);
				errors.add(new SummaryError(owner.owner(), error.status(), error.message()));
			}
		}

		return new SectionResult<>(incomesSummary(availableDetail, transactionsDetail), errors);
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

	private IncomesSummary incomesSummary(Map<String, List<IncomeAccount>> availableDetail,
			Map<String, List<IncomeTransaction>> transactionsDetail) {
		var totalBalance = SummaryTotals.brl(availableDetail.values().stream().flatMap(List::stream)
				.map(IncomeAccount::balance).toList());

		var totalTransactions = SummaryTotals.brl(transactionsDetail.values().stream().flatMap(List::stream)
				.map(IncomeTransaction::amount).toList());

		return new IncomesSummary(
				new SummarySection<>(totalBalance, availableDetail),
				new SummarySection<>(totalTransactions, transactionsDetail));
	}

	/** Entradas do período de todas as contas do dono. Qualquer falha descarta o dono inteiro. */
	private List<IncomeTransaction> incomesOf(String owner, List<Account> accounts, LocalDate from, LocalDate to) {
		List<IncomeTransaction> incomes = new ArrayList<>();
		for (Account account : accounts) {
			for (Transaction tx : transactionService.getAllTransactions(owner, account.id(), from, to)) {
				if (tx.isBrlCredit()) {
					incomes.add(new IncomeTransaction(tx.id(), tx.description(), tx.amount(), tx.currencyCode(),
							tx.date(), account.id()));
				}
			}
		}
		return incomes;
	}

}
