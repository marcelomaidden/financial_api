package com.mfaraujo.financialapi.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyCursorPage;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.config.PluggyProperties;
import com.mfaraujo.financialapi.exception.AllOwnersFailedException;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.BillResponse;
import com.mfaraujo.financialapi.resource.dto.BillsResponse;
import com.mfaraujo.financialapi.resource.dto.CreditCardResponse;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Consulta a Pluggy em nome de cada dono configurado e traduz o formato da
 * Pluggy para o contrato da API.
 */
@Service
public class PluggyService {

	private static final Logger log = LoggerFactory.getLogger(PluggyService.class);

	private final PluggyClient pluggyClient;

	private final PluggyProperties properties;

	public PluggyService(PluggyClient pluggyClient, PluggyProperties properties) {
		this.pluggyClient = pluggyClient;
		this.properties = properties;
	}

	/**
	 * Contas de todos os donos, ou só de {@code owner} quando informado.
	 * <p>
	 * Sem filtro, a falha de um dono vira erro na entrada dele e não descarta os
	 * demais. Se todos falharem, lança {@link AllOwnersFailedException} (502).
	 * Com filtro, a falha segue o fluxo normal de erro.
	 */
	public OwnersResponse<OwnerAccounts> getAccounts(String owner) {
		if (owner != null) {
			requireOwner(owner);
			return new OwnersResponse<>(List.of(new OwnerAccounts(owner, toAccounts(accountsOf(owner)), null)));
		}
		List<OwnerAccounts> results = new ArrayList<>();
		int failures = 0;
		for (PluggyProperties.Owner configured : properties.owners()) {
			String name = configured.name();
			try {
				results.add(new OwnerAccounts(name, toAccounts(accountsOf(name)), null));
			}
			catch (RuntimeException ex) {
				failures++;
				results.add(new OwnerAccounts(name, List.of(), logAndConvert(name, ex)));
			}
		}
		if (failures == properties.owners().size()) {
			throw new AllOwnersFailedException();
		}
		return new OwnersResponse<>(results);
	}

	/**
	 * Cartões de crédito de todos os donos, ou só de {@code owner} quando
	 * informado. Mesmas regras de falha de {@link #getAccounts(String)}.
	 */
	public OwnersResponse<OwnerCreditCards> getCreditCards(String owner) {
		if (owner != null) {
			requireOwner(owner);
			return new OwnersResponse<>(List.of(new OwnerCreditCards(owner, toCreditCards(accountsOf(owner)), null)));
		}
		List<OwnerCreditCards> results = new ArrayList<>();
		int failures = 0;
		for (PluggyProperties.Owner configured : properties.owners()) {
			String name = configured.name();
			try {
				results.add(new OwnerCreditCards(name, toCreditCards(accountsOf(name)), null));
			}
			catch (RuntimeException ex) {
				failures++;
				results.add(new OwnerCreditCards(name, List.of(), logAndConvert(name, ex)));
			}
		}
		if (failures == properties.owners().size()) {
			throw new AllOwnersFailedException();
		}
		return new OwnersResponse<>(results);
	}

	public TransactionsResponse getTransactions(String owner, String accountId, LocalDate dateFrom, LocalDate dateTo,
			String after) {
		requireOwner(owner);
		PluggyCursorPage<PluggyTransaction> page = pluggyClient.listTransactions(owner, accountId, dateFrom, dateTo,
				after);
		List<TransactionResponse> transactions = page.resultsOrEmpty().stream().map(this::toTransaction).toList();
		return new TransactionsResponse(owner, accountId, transactions, cursorFrom(page.next()));
	}

	public BillsResponse getBills(String owner, String accountId, Integer page, Integer pageSize) {
		requireOwner(owner);
		PluggyPage<PluggyBill> bills = pluggyClient.listBills(owner, accountId, page, pageSize);
		return new BillsResponse(owner, accountId, bills.resultsOrEmpty().stream().map(this::toBill).toList(),
				bills.page(), bills.totalPages());
	}

	/** Registra a falha de um dono e a converte no erro que vai na entrada dele. */
	private ApiError logAndConvert(String owner, RuntimeException ex) {
		ApiError error = ApiErrors.from(ex);
		log.warn("Consulta à Pluggy falhou para o dono '{}': {} {}", owner, error.status(), error.message());
		log.debug("Detalhe da falha do dono '{}'", owner, ex);
		return error;
	}

	private List<AccountResponse> toAccounts(List<PluggyAccount> accounts) {
		return accounts.stream().map(this::toAccount).toList();
	}

	private List<CreditCardResponse> toCreditCards(List<PluggyAccount> accounts) {
		return accounts.stream().filter(PluggyAccount::isCreditCard).map(this::toCreditCard).toList();
	}

	private List<PluggyAccount> accountsOf(String owner) {
		List<String> itemIds = properties.findOwner(owner).orElseThrow(() -> new UnknownOwnerException(owner)).itemIds();
		return itemIds.stream()
				.map(itemId -> pluggyClient.listAccounts(owner, itemId))
				.map(PluggyPage::resultsOrEmpty)
				.flatMap(List::stream)
				.toList();
	}

	private void requireOwner(String owner) {
		if (properties.findOwner(owner).isEmpty()) {
			throw new UnknownOwnerException(owner);
		}
	}

	/** Extrai o valor de {@code after} da query string {@code next} da Pluggy. */
	static String cursorFrom(String next) {
		if (next == null || next.isBlank()) {
			return null;
		}
		String query = next.startsWith("?") ? next.substring(1) : next;
		for (String pair : query.split("&")) {
			int eq = pair.indexOf('=');
			if (eq > 0 && pair.substring(0, eq).equals("after")) {
				return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	private AccountResponse toAccount(PluggyAccount account) {
		return new AccountResponse(account.id(), account.itemId(), account.type(), account.subtype(),
				account.name(), account.number(), account.balance(), account.currencyCode());
	}

	private CreditCardResponse toCreditCard(PluggyAccount account) {
		PluggyAccount.CreditData credit = account.creditData();
		return new CreditCardResponse(account.id(), account.itemId(), account.name(), account.number(),
				credit == null ? null : credit.brand(), account.balance(), account.currencyCode(),
				credit == null ? null : credit.creditLimit(),
				credit == null ? null : credit.availableCreditLimit(),
				credit == null ? null : credit.balanceCloseDate(),
				credit == null ? null : credit.balanceDueDate());
	}

	private TransactionResponse toTransaction(PluggyTransaction tx) {
		return new TransactionResponse(tx.id(), tx.date(), tx.description(), tx.amount(), tx.currencyCode(),
				tx.type(), tx.status(), tx.category());
	}

	private BillResponse toBill(PluggyBill bill) {
		return new BillResponse(bill.id(), bill.dueDate(), bill.billClosingDate(), bill.totalAmount(),
				bill.totalAmountCurrencyCode(), bill.minimumPaymentAmount());
	}

}
