package com.mfaraujo.financialapi.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.resource.dto.ExpensesSummary;
import com.mfaraujo.financialapi.resource.dto.IncomesSummary;
import com.mfaraujo.financialapi.resource.dto.SummaryError;
import com.mfaraujo.financialapi.resource.dto.SummaryResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Ponto de entrada de {@code GET /summary}. Define os períodos, busca as contas uma
 * única vez, registra a falha de contas de cada dono e junta as seções
 * ({@code incomes} e {@code expenses}). Erros idênticos são unificados.
 * <p>
 * Um dono cuja consulta de contas falhou entra em {@code errors} uma vez e não é
 * repassado às seções. Se a consulta de contas falhar para todos,
 * {@code AccountService} lança {@code AllOwnersFailedException} (502).
 */
@Service
public class SummaryService {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private final AccountService accountService;

	private final IncomesSummaryService incomesSummaryService;

	private final ExpensesSummaryService expensesSummaryService;

	private final Clock clock;

	@Autowired
	public SummaryService(AccountService accountService, IncomesSummaryService incomesSummaryService,
			ExpensesSummaryService expensesSummaryService) {
		this(accountService, incomesSummaryService, expensesSummaryService, Clock.system(ZONE));
	}

	SummaryService(AccountService accountService, IncomesSummaryService incomesSummaryService,
			ExpensesSummaryService expensesSummaryService, Clock clock) {
		this.accountService = accountService;
		this.incomesSummaryService = incomesSummaryService;
		this.expensesSummaryService = expensesSummaryService;
		this.clock = clock;
	}

	public SummaryResponse getSummary(LocalDate dateFrom, LocalDate dateTo) {
		LocalDate to = dateTo == null ? LocalDate.now(clock) : dateTo;
		LocalDate monthStart = to.withDayOfMonth(1);
		LocalDate from = dateFrom == null ? monthStart : dateFrom;
		ExpensesPeriod expensesPeriod = new ExpensesPeriod(from, YearMonth.from(to).atEndOfMonth(),
				dateFrom == null ? monthStart.minusMonths(1) : dateFrom, to);

		List<SummaryError> errors = new ArrayList<>();
		List<OwnerAccountsResult> owners = new ArrayList<>();
		for (OwnerAccountsResult owner : accountService.getAccounts(null).owners()) {
			if (owner.error() != null) {
				errors.add(new SummaryError(owner.owner(), owner.error().status(), owner.error().message()));
			}
			else {
				owners.add(owner);
			}
		}

		SectionResult<IncomesSummary> incomes = incomesSummaryService.summarize(owners, from, to);
		SectionResult<ExpensesSummary> expenses = expensesSummaryService.summarize(owners, expensesPeriod);
		errors.addAll(incomes.errors());
		errors.addAll(expenses.errors());

		List<SummaryError> distinctErrors = errors.stream().distinct().toList();
		return new SummaryResponse(incomes.section(), expenses.section(), !distinctErrors.isEmpty(), distinctErrors);
	}

}
