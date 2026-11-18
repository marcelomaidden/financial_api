package com.mfaraujo.financialapi.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyCursorPage;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.TransactionPage;

import org.springframework.stereotype.Service;

/** Transações de uma conta de um dono. */
@Service
public class TransactionService {

	private final PluggyClient pluggyClient;

	private final OwnerRegistry owners;

	public TransactionService(PluggyClient pluggyClient, OwnerRegistry owners) {
		this.pluggyClient = pluggyClient;
		this.owners = owners;
	}

	public TransactionPage getTransactions(String owner, String accountId, LocalDate dateFrom, LocalDate dateTo,
			String after) {
		owners.require(owner);
		PluggyCursorPage<PluggyTransaction> page = pluggyClient.listTransactions(owner, accountId, dateFrom, dateTo,
				after);
		return toPage(page);
	}

	/** Uma página das transações que chegaram à Pluggy a partir de {@code createdAtFrom}. */
	TransactionPage getCreatedSince(String owner, String accountId, Instant createdAtFrom, String after) {
		owners.require(owner);
		return toPage(pluggyClient.listTransactionsCreatedSince(owner, accountId, createdAtFrom, after));
	}

	/** Uma página das compras de uma fatura de cartão. */
	TransactionPage getByBill(String owner, String accountId, String billId, String after) {
		owners.require(owner);
		return toPage(pluggyClient.listTransactionsByBill(owner, accountId, billId, after));
	}

	private static TransactionPage toPage(PluggyCursorPage<PluggyTransaction> page) {
		List<Transaction> transactions = page.resultsOrEmpty().stream().map(PluggyMapper::toTransaction).toList();
		return new TransactionPage(transactions, cursorFrom(page.next()));
	}

	/** Todas as transações do período, seguindo {@code nextCursor} até a última página. */
	public List<Transaction> getAllTransactions(String owner, String accountId, LocalDate dateFrom, LocalDate dateTo) {
		return drain(after -> getTransactions(owner, accountId, dateFrom, dateTo, after));
	}

	/** Todas as transações que chegaram à Pluggy a partir de {@code createdAtFrom}, em todas as páginas. */
	public List<Transaction> getAllCreatedSince(String owner, String accountId, Instant createdAtFrom) {
		return drain(after -> getCreatedSince(owner, accountId, createdAtFrom, after));
	}

	/** Todas as compras da fatura, em todas as páginas. */
	public List<Transaction> getAllByBill(String owner, String accountId, String billId) {
		return drain(after -> getByBill(owner, accountId, billId, after));
	}

	/** Busca páginas, começando sem cursor, até a última. */
	private static List<Transaction> drain(Function<String, TransactionPage> fetch) {
		List<Transaction> all = new ArrayList<>();
		String after = null;
		do {
			TransactionPage page = fetch.apply(after);
			all.addAll(page.transactions());
			after = page.nextCursor();
		}
		while (after != null);
		return all;
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

}
