package com.mfaraujo.financialapi.service;

import java.util.ArrayList;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.result.BillPage;

import org.springframework.stereotype.Service;

/** Faturas de uma conta de cartão de um dono. */
@Service
public class BillService {

	static final int PAGE_SIZE = 500;

	private final PluggyClient pluggyClient;

	private final OwnerRegistry owners;

	public BillService(PluggyClient pluggyClient, OwnerRegistry owners) {
		this.pluggyClient = pluggyClient;
		this.owners = owners;
	}

	/** Todas as faturas da conta, percorrendo as páginas até a última. */
	public List<Bill> getAllBills(String owner, String accountId) {
		List<Bill> all = new ArrayList<>();
		int page = 1;
		while (true) {
			BillPage current = getBills(owner, accountId, page, PAGE_SIZE);
			all.addAll(current.bills());
			Integer totalPages = current.totalPages();
			if (current.bills().isEmpty() || totalPages == null || page >= totalPages) {
				return all;
			}
			page++;
		}
	}

	public BillPage getBills(String owner, String accountId, Integer page, Integer pageSize) {
		owners.require(owner);
		PluggyPage<PluggyBill> bills = pluggyClient.listBills(owner, accountId, page, pageSize);
		return new BillPage(bills.resultsOrEmpty().stream().map(PluggyMapper::toBill).toList(), bills.page(),
				bills.totalPages());
	}

}
