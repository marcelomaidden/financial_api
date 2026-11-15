package com.mfaraujo.financialapi.service;

import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
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
	public OwnersResponse<OwnerAccounts> getAccounts(String owner) {
		return fanOut.collect(owner,
				name -> new OwnerAccounts(name, accountsOf(name).stream().map(PluggyMapper::toAccount).toList(), null),
				(name, error) -> new OwnerAccounts(name, List.of(), error));
	}

	/** Cartões de crédito de todos os donos, ou só de {@code owner} quando informado. */
	public OwnersResponse<OwnerCreditCards> getCreditCards(String owner) {
		return fanOut.collect(owner,
				name -> new OwnerCreditCards(name, accountsOf(name).stream()
						.filter(PluggyAccount::isCreditCard)
						.map(PluggyMapper::toCreditCard)
						.toList(), null),
				(name, error) -> new OwnerCreditCards(name, List.of(), error));
	}

	private List<PluggyAccount> accountsOf(String owner) {
		return owners.require(owner).itemIds().stream()
				.map(itemId -> pluggyClient.listAccounts(owner, itemId))
				.map(PluggyPage::resultsOrEmpty)
				.flatMap(List::stream)
				.toList();
	}

}
