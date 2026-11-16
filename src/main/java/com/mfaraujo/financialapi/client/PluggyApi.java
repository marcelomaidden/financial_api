package com.mfaraujo.financialapi.client;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyCursorPage;
import com.mfaraujo.financialapi.client.dto.PluggyLoan;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Endpoints de leitura da Pluggy. Cada instância é ligada às credenciais de um
 * dono pelo {@link PluggyApiKeyInterceptor}.
 */
@HttpExchange(accept = "application/json")
public interface PluggyApi {

	@GetExchange("/accounts")
	PluggyPage<PluggyAccount> listAccounts(@RequestParam("itemId") String itemId);

	@GetExchange("/v2/transactions")
	PluggyCursorPage<PluggyTransaction> listTransactions(
			@RequestParam("accountId") String accountId,
			@RequestParam(name = "dateFrom", required = false) String dateFrom,
			@RequestParam(name = "dateTo", required = false) String dateTo,
			@RequestParam(name = "after", required = false) String after);

	/** Transações que chegaram à Pluggy a partir de {@code createdAtFrom}. Não combina com {@code dateFrom}. */
	@GetExchange("/v2/transactions")
	PluggyCursorPage<PluggyTransaction> listTransactionsCreatedSince(
			@RequestParam("accountId") String accountId,
			@RequestParam("createdAtFrom") String createdAtFrom,
			@RequestParam(name = "after", required = false) String after);

	@GetExchange("/loans")
	PluggyPage<PluggyLoan> listLoans(@RequestParam("itemId") String itemId);

	@GetExchange("/bills")
	PluggyPage<PluggyBill> listBills(
			@RequestParam("accountId") String accountId,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize);

}
