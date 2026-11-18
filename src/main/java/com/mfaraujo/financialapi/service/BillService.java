package com.mfaraujo.financialapi.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mfaraujo.financialapi.client.PluggyClient;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.BillDetailResult;
import com.mfaraujo.financialapi.domain.result.BillPage;

import org.springframework.stereotype.Service;

/** Faturas de uma conta de cartão de um dono. */
@Service
public class BillService {

	static final int PAGE_SIZE = 500;

	private final PluggyClient pluggyClient;

	private final OwnerRegistry owners;

	private final TransactionService transactionService;

	public BillService(PluggyClient pluggyClient, OwnerRegistry owners, TransactionService transactionService) {
		this.pluggyClient = pluggyClient;
		this.owners = owners;
		this.transactionService = transactionService;
	}

	/**
	 * Detalhe de uma fatura com as compras dela, da mais recente para a mais antiga. Qualquer
	 * falha da Pluggy sobe sem resposta parcial.
	 */
	public BillDetailResult getBillDetail(String owner, String id, String accountId) {
		owners.require(owner);
		var bill = PluggyMapper.toBillDetail(pluggyClient.retrieveBill(owner, id));
		List<Transaction> transactions = new ArrayList<>(transactionService.getAllByBill(owner, accountId, id));
		transactions.sort(Comparator.comparing(Transaction::date, Comparator.nullsLast(Comparator.reverseOrder())));
		return new BillDetailResult(bill, transactions);
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
