package com.mfaraujo.financialapi.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

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
		List<Transaction> transactions = page.resultsOrEmpty().stream().map(PluggyMapper::toTransaction).toList();
		return new TransactionPage(transactions, cursorFrom(page.next()));
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
