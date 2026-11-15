package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.exception.AllOwnersFailedException;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.ExpensesSummary;
import com.mfaraujo.financialapi.resource.dto.IncomesSummary;
import com.mfaraujo.financialapi.resource.dto.Money;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.SummaryError;
import com.mfaraujo.financialapi.resource.dto.SummaryResponse;
import com.mfaraujo.financialapi.resource.dto.SummarySection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SummaryServiceTest {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

	private static final Money ZERO = new Money(BigDecimal.ZERO, "BRL");

	private static final IncomesSummary INCOMES = new IncomesSummary(new SummarySection<>(ZERO, Map.of()),
			new SummarySection<>(ZERO, Map.of()));

	private static final ExpensesSummary EXPENSES = new ExpensesSummary(new SummarySection<>(ZERO, Map.of()),
			new SummarySection<>(ZERO, Map.of()));

	private AccountService accountService;

	private IncomesSummaryService incomesService;

	private ExpensesSummaryService expensesService;

	private SummaryService service;

	@BeforeEach
	void setUp() {
		accountService = mock(AccountService.class);
		incomesService = mock(IncomesSummaryService.class);
		expensesService = mock(ExpensesSummaryService.class);
		Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
		service = new SummaryService(accountService, incomesService, expensesService, clock);
		when(incomesService.summarize(any(), any(), any())).thenReturn(new SectionResult<>(INCOMES, List.of()));
		when(expensesService.summarize(any(), any())).thenReturn(new SectionResult<>(EXPENSES, List.of()));
	}

	@Test
	void fetchesAccountsOnceAndReturnsBothSections() {
		accounts(owner("marcelo"), owner("luana"));

		SummaryResponse summary = service.getSummary(null, null);

		verify(accountService, times(1)).getAccounts(null);
		assertThat(summary.incomes()).isSameAs(INCOMES);
		assertThat(summary.expenses()).isSameAs(EXPENSES);
		assertThat(summary.partial()).isFalse();
		assertThat(summary.errors()).isEmpty();
	}

	@Test
	void recordsAccountErrorOnceAndDoesNotPassTheOwnerToAnySection() {
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");
		accounts(new OwnerAccountsResult("luana", List.of(), error), owner("marcelo"));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isTrue();
		assertThat(summary.errors()).containsExactly(new SummaryError("luana", 502, "Erro ao consultar a Pluggy"));
		ArgumentCaptor<List<OwnerAccountsResult>> incomesOwners = ArgumentCaptor.captor();
		verify(incomesService).summarize(incomesOwners.capture(), any(), any());
		assertThat(incomesOwners.getValue()).extracting(OwnerAccountsResult::owner).containsExactly("marcelo");
		ArgumentCaptor<List<OwnerAccountsResult>> expensesOwners = ArgumentCaptor.captor();
		verify(expensesService).summarize(expensesOwners.capture(), any());
		assertThat(expensesOwners.getValue()).extracting(OwnerAccountsResult::owner).containsExactly("marcelo");
	}

	@Test
	void putsAccountErrorsBeforeIncomesErrorsBeforeExpensesErrors() {
		ApiError error = new ApiError(502, "Bad Gateway", "contas");
		accounts(owner("marcelo"), new OwnerAccountsResult("luana", List.of(), error));
		when(incomesService.summarize(any(), any(), any()))
				.thenReturn(new SectionResult<>(INCOMES, List.of(new SummaryError("marcelo", 500, "incomes"))));
		when(expensesService.summarize(any(), any()))
				.thenReturn(new SectionResult<>(EXPENSES, List.of(new SummaryError("marcelo", 500, "expenses"))));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isTrue();
		assertThat(summary.errors()).extracting(SummaryError::message).containsExactly("contas", "incomes", "expenses");
	}

	@Test
	void mergesIdenticalErrorsOnly() {
		SummaryError same = new SummaryError("marcelo", 502, "Erro ao consultar a Pluggy");
		accounts(owner("marcelo"));
		when(incomesService.summarize(any(), any(), any()))
				.thenReturn(new SectionResult<>(INCOMES, List.of(same)));
		when(expensesService.summarize(any(), any())).thenReturn(new SectionResult<>(EXPENSES,
				List.of(same, new SummaryError("marcelo", 502, "outro motivo"), same)));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.errors()).containsExactly(same, new SummaryError("marcelo", 502, "outro motivo"));
	}

	@Test
	void propagatesFailureWhenAllOwnersFail() {
		when(accountService.getAccounts(null)).thenThrow(new AllOwnersFailedException());

		assertThatThrownBy(() -> service.getSummary(null, null)).isInstanceOf(AllOwnersFailedException.class);
		verify(incomesService, never()).summarize(any(), any(), any());
		verify(expensesService, never()).summarize(any(), any());
	}

	@Test
	void defaultsToCurrentMonthWithoutParameters() {
		accounts(owner("marcelo"));

		service.getSummary(null, null);

		verifyIncomesPeriod(LocalDate.of(2026, 11, 1), TODAY);
		assertThat(expensesPeriod()).isEqualTo(new ExpensesPeriod(LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 30), LocalDate.of(2026, 10, 1), TODAY));
	}

	@Test
	void usesDateFromUntilToday() {
		accounts(owner("marcelo"));
		LocalDate from = LocalDate.of(2026, 10, 10);

		service.getSummary(from, null);

		verifyIncomesPeriod(from, TODAY);
		assertThat(expensesPeriod()).isEqualTo(new ExpensesPeriod(from, LocalDate.of(2026, 11, 30), from, TODAY));
	}

	@Test
	void usesFirstDayOfDateToMonthUntilDateTo() {
		accounts(owner("marcelo"));
		LocalDate to = LocalDate.of(2026, 9, 20);

		service.getSummary(null, to);

		verifyIncomesPeriod(LocalDate.of(2026, 9, 1), to);
		assertThat(expensesPeriod()).isEqualTo(new ExpensesPeriod(LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30), LocalDate.of(2026, 8, 1), to));
	}

	@Test
	void usesBothDatesWhenGiven() {
		accounts(owner("marcelo"));
		LocalDate from = LocalDate.of(2026, 8, 5);
		LocalDate to = LocalDate.of(2026, 9, 20);

		service.getSummary(from, to);

		verifyIncomesPeriod(from, to);
		assertThat(expensesPeriod())
				.isEqualTo(new ExpensesPeriod(from, LocalDate.of(2026, 9, 30), from, to));
	}

	@Test
	void dueDateWindowEndsOnTheLastDayOfAShortMonth() {
		accounts(owner("marcelo"));

		service.getSummary(null, LocalDate.of(2028, 2, 10));

		assertThat(expensesPeriod().dueTo()).isEqualTo(LocalDate.of(2028, 2, 29));
	}

	private void verifyIncomesPeriod(LocalDate from, LocalDate to) {
		verify(incomesService).summarize(any(), eq(from), eq(to));
	}

	private ExpensesPeriod expensesPeriod() {
		ArgumentCaptor<ExpensesPeriod> period = ArgumentCaptor.forClass(ExpensesPeriod.class);
		verify(expensesService).summarize(any(), period.capture());
		return period.getValue();
	}

	private void accounts(OwnerAccountsResult... owners) {
		when(accountService.getAccounts(null)).thenReturn(new OwnersResponse<>(List.of(owners)));
	}

	private static OwnerAccountsResult owner(String name) {
		return new OwnerAccountsResult(name, List.of(), null);
	}

}
