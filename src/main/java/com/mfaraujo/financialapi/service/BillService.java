package com.mfaraujo.financialapi.service;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.resource.dto.BillsResponse;

import org.springframework.stereotype.Service;

/** Faturas de uma conta de cartão de um dono. */
@Service
public class BillService {

	private final PluggyClient pluggyClient;

	private final OwnerRegistry owners;

	public BillService(PluggyClient pluggyClient, OwnerRegistry owners) {
		this.pluggyClient = pluggyClient;
		this.owners = owners;
	}

	public BillsResponse getBills(String owner, String accountId, Integer page, Integer pageSize) {
		owners.require(owner);
		PluggyPage<PluggyBill> bills = pluggyClient.listBills(owner, accountId, page, pageSize);
		return new BillsResponse(owner, accountId, bills.resultsOrEmpty().stream().map(PluggyMapper::toBill).toList(),
				bills.page(), bills.totalPages());
	}

}
