package com.mfaraujo.financialapi.resource;

import java.time.LocalDate;

import com.mfaraujo.financialapi.resource.dto.BillsResponse;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
import com.mfaraujo.financialapi.resource.dto.OwnerLoans;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.SummaryResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;
import com.mfaraujo.financialapi.service.AccountService;
import com.mfaraujo.financialapi.service.BillService;
import com.mfaraujo.financialapi.service.LoanService;
import com.mfaraujo.financialapi.service.SummaryService;
import com.mfaraujo.financialapi.service.TransactionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FinancialDataController {

	private final AccountService accountService;

	private final TransactionService transactionService;

	private final BillService billService;

	private final SummaryService summaryService;

	private final LoanService loanService;

	public FinancialDataController(AccountService accountService, TransactionService transactionService,
			BillService billService, SummaryService summaryService, LoanService loanService) {
		this.accountService = accountService;
		this.transactionService = transactionService;
		this.billService = billService;
		this.summaryService = summaryService;
		this.loanService = loanService;
	}

	@GetMapping("/summary")
	public SummaryResponse summary(
			@RequestParam(required = false) LocalDate dateFrom,
			@RequestParam(required = false) LocalDate dateTo
	) {
		return summaryService.getSummary(dateFrom, dateTo);
	}

	@GetMapping("/accounts")
	public OwnersResponse<OwnerAccounts> accounts(@RequestParam(required = false) String owner) {
		return ResponseMapper.toAccounts(accountService.getAccounts(owner));
	}

	@GetMapping("/loans")
	public OwnersResponse<OwnerLoans> loans(@RequestParam(required = false) String owner) {
		return ResponseMapper.toLoans(loanService.getLoans(owner));
	}

	@GetMapping("/credit-cards")
	public OwnersResponse<OwnerCreditCards> creditCards(@RequestParam(required = false) String owner) {
		return ResponseMapper.toCreditCards(accountService.getCreditCards(owner));
	}

	/**
	 * Transações de uma conta. Período em {@code dateFrom}/{@code dateTo}
	 * ({@code yyyy-mm-dd}); próxima página com {@code after} = {@code nextCursor}
	 * da resposta anterior.
	 */
	@GetMapping("/owners/{owner}/accounts/{accountId}/transactions")
	public TransactionsResponse transactions(@PathVariable String owner, @PathVariable String accountId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
			@RequestParam(required = false) String after) {
		return ResponseMapper.toTransactions(owner, accountId,
				transactionService.getTransactions(owner, accountId, dateFrom, dateTo, after));
	}

	@GetMapping("/owners/{owner}/accounts/{accountId}/bills")
	public BillsResponse bills(@PathVariable String owner, @PathVariable String accountId,
			@RequestParam(required = false) @Min(1) Integer page,
			@RequestParam(required = false) @Min(1) @Max(500) Integer pageSize) {
		return ResponseMapper.toBills(owner, accountId, billService.getBills(owner, accountId, page, pageSize));
	}

}
