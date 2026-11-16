package com.mfaraujo.financialapi.service;

import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.domain.model.Loan;
import com.mfaraujo.financialapi.domain.result.OwnerLoansResult;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;

import org.springframework.stereotype.Service;

/** Empréstimos dos donos, consultados por item. */
@Service
public class LoanService {

	private final PluggyClient pluggyClient;

	private final OwnerRegistry owners;

	private final OwnerFanOut fanOut;

	public LoanService(PluggyClient pluggyClient, OwnerRegistry owners, OwnerFanOut fanOut) {
		this.pluggyClient = pluggyClient;
		this.owners = owners;
		this.fanOut = fanOut;
	}

	/** Empréstimos de todos os donos, ou só de {@code owner} quando informado. */
	public OwnersResponse<OwnerLoansResult> getLoans(String owner) {
		return fanOut.collect(owner,
				name -> new OwnerLoansResult(name, loansOf(name), null),
				(name, error) -> new OwnerLoansResult(name, List.of(), error));
	}

	private List<Loan> loansOf(String owner) {
		return owners.require(owner).itemIds().stream()
				.map(itemId -> pluggyClient.listLoans(owner, itemId))
				.map(PluggyPage::resultsOrEmpty)
				.flatMap(List::stream)
				.map(PluggyMapper::toLoan)
				.toList();
	}

}
