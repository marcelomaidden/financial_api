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

	private static final LocalDate MONTH_START = LocalDate.of(2026, 11, 1);

	private static final IncomesSummary EMPTY = new IncomesSummary(
			new SummarySection<>(new Money(BigDecimal.ZERO, "BRL"), Map.of()),
			new SummarySection<>(new Money(BigDecimal.ZERO, "BRL"), Map.of()));

	private AccountService accountService;

	private IncomesSummaryService incomesService;

	private SummaryService service;

	@BeforeEach
	void setUp() {
		accountService = mock(AccountService.class);
		incomesService = mock(IncomesSummaryService.class);
		Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
		service = new SummaryService(accountService, incomesService, clock);
		when(incomesService.summarize(any(), any(), any())).thenReturn(new SectionResult<>(EMPTY, List.of()));
	}

	@Test
	void fetchesAccountsOnceAndReturnsTheSection() {
		accounts(owner("marcelo"), owner("luana"));

		SummaryResponse summary = service.getSummary(null, null);

		verify(accountService, times(1)).getAccounts(null);
		assertThat(summary.incomes()).isSameAs(EMPTY);
		assertThat(summary.partial()).isFalse();
		assertThat(summary.errors()).isEmpty();
	}

	@Test
	void recordsAccountErrorOnceAndDoesNotPassTheOwnerToTheSection() {
		ApiError error = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");
		accounts(new OwnerAccountsResult("luana", List.of(), error), owner("marcelo"));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isTrue();
		assertThat(summary.errors()).containsExactly(new SummaryError("luana", 502, "Erro ao consultar a Pluggy"));
		ArgumentCaptor<List<OwnerAccountsResult>> owners = ArgumentCaptor.captor();
		verify(incomesService).summarize(owners.capture(), any(), any());
		assertThat(owners.getValue()).extracting(OwnerAccountsResult::owner).containsExactly("marcelo");
	}

	@Test
	void putsAccountErrorsBeforeSectionErrors() {
		ApiError error = new ApiError(502, "Bad Gateway", "contas");
		accounts(owner("marcelo"), new OwnerAccountsResult("luana", List.of(), error));
		when(incomesService.summarize(any(), any(), any()))
				.thenReturn(new SectionResult<>(EMPTY, List.of(new SummaryError("marcelo", 500, "transações"))));

		SummaryResponse summary = service.getSummary(null, null);

		assertThat(summary.partial()).isTrue();
		assertThat(summary.errors()).extracting(SummaryError::owner).containsExactly("luana", "marcelo");
	}

	@Test
	void propagatesFailureWhenAllOwnersFail() {
		when(accountService.getAccounts(null)).thenThrow(new AllOwnersFailedException());

		assertThatThrownBy(() -> service.getSummary(null, null)).isInstanceOf(AllOwnersFailedException.class);
		verify(incomesService, never()).summarize(any(), any(), any());
	}

	@Test
	void defaultsToCurrentMonthWithoutParameters() {
		accounts(owner("marcelo"));

		service.getSummary(null, null);

		verify(incomesService).summarize(any(), eq(MONTH_START),
				eq(TODAY));
	}

	@Test
	void usesDateFromUntilToday() {
		accounts(owner("marcelo"));
		LocalDate from = LocalDate.of(2026, 10, 10);

		service.getSummary(from, null);

		verify(incomesService).summarize(any(), eq(from),
				eq(TODAY));
	}

	@Test
	void usesFirstDayOfDateToMonthUntilDateTo() {
		accounts(owner("marcelo"));
		LocalDate to = LocalDate.of(2026, 9, 20);

		service.getSummary(null, to);

		verify(incomesService).summarize(any(), eq(LocalDate.of(2026, 9, 1)),
				eq(to));
	}

	@Test
	void usesBothDatesWhenGiven() {
		accounts(owner("marcelo"));
		LocalDate from = LocalDate.of(2026, 8, 5);
		LocalDate to = LocalDate.of(2026, 9, 20);

		service.getSummary(from, to);

		verify(incomesService).summarize(any(), eq(from),
				eq(to));
	}

	private void accounts(OwnerAccountsResult... owners) {
		when(accountService.getAccounts(null)).thenReturn(new OwnersResponse<>(List.of(owners)));
	}

	private static OwnerAccountsResult owner(String name) {
		return new OwnerAccountsResult(name, List.of(), null);
	}

}
