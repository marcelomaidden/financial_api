package com.mfaraujo.financialapi.service;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.BillResponse;
import com.mfaraujo.financialapi.resource.dto.CreditCardResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;

/** Traduz os DTOs da Pluggy para o contrato da API. */
final class PluggyMapper {

	private PluggyMapper() {
	}

	static AccountResponse toAccount(PluggyAccount account) {
		return new AccountResponse(account.id(), account.itemId(), account.type(), account.subtype(),
				account.name(), account.number(), account.balance(), account.currencyCode());
	}

	static CreditCardResponse toCreditCard(PluggyAccount account) {
		PluggyAccount.CreditData credit = account.creditData();
		return new CreditCardResponse(account.id(), account.itemId(), account.name(), account.number(),
				credit == null ? null : credit.brand(), account.balance(), account.currencyCode(),
				credit == null ? null : credit.creditLimit(),
				credit == null ? null : credit.availableCreditLimit(),
				credit == null ? null : credit.balanceCloseDate(),
				credit == null ? null : credit.balanceDueDate());
	}

	static TransactionResponse toTransaction(PluggyTransaction tx) {
		return new TransactionResponse(tx.id(), tx.date(), tx.description(), tx.amount(), tx.currencyCode(),
				tx.type(), tx.status(), tx.category());
	}

	static BillResponse toBill(PluggyBill bill) {
		return new BillResponse(bill.id(), bill.dueDate(), bill.billClosingDate(), bill.totalAmount(),
				bill.totalAmountCurrencyCode(), bill.minimumPaymentAmount());
	}

}
