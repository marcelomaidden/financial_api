package com.mfaraujo.financialapi.service;

import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.OwnerCreditCardsResult;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;

import org.springframework.stereotype.Service;

/**
 * Contas e cartões de crédito dos donos. Cartão é uma conta filtrada por tipo,
 * então os dois partem da mesma consulta.
 */
@Service
public class AccountService {

	private final PluggyClient pluggyClient;

	private final OwnerRegistry owners;

	private final OwnerFanOut fanOut;

	public AccountService(PluggyClient pluggyClient, OwnerRegistry owners, OwnerFanOut fanOut) {
		this.pluggyClient = pluggyClient;
		this.owners = owners;
		this.fanOut = fanOut;
	}

	/** Contas de todos os donos, ou só de {@code owner} quando informado. */
	public OwnersResponse<OwnerAccountsResult> getAccounts(String owner) {
		return fanOut.collect(owner,
				name -> new OwnerAccountsResult(name, accountsOf(name), null),
				(name, error) -> new OwnerAccountsResult(name, List.of(), error));
	}

	/** Cartões de crédito de todos os donos, ou só de {@code owner} quando informado. */
	public OwnersResponse<OwnerCreditCardsResult> getCreditCards(String owner) {
		return fanOut.collect(owner,
				name -> new OwnerCreditCardsResult(name, accountsOf(name).stream()
						.filter(Account::isCreditCard)
						.toList(), null),
				(name, error) -> new OwnerCreditCardsResult(name, List.of(), error));
	}

	private List<Account> accountsOf(String owner) {
		return owners.require(owner).itemIds().stream()
				.map(itemId -> pluggyClient.listAccounts(owner, itemId))
				.map(PluggyPage::resultsOrEmpty)
				.flatMap(List::stream)
				.map(PluggyMapper::toAccount)
				.toList();
	}

}
