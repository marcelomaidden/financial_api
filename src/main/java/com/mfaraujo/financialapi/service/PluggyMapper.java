package com.mfaraujo.financialapi.service;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;

/** Traduz os DTOs da Pluggy para o modelo de domínio. */
final class PluggyMapper {

	private PluggyMapper() {
	}

	static Account toAccount(PluggyAccount account) {
		PluggyAccount.CreditData credit = account.creditData();
		return new Account(account.id(), account.itemId(), AccountTypesEnum.fromPluggy(account.type()),
				account.subtype(), account.name(), account.number(), account.balance(), account.currencyCode(),
				credit == null ? null
						: new Account.Credit(credit.brand(), credit.creditLimit(), credit.availableCreditLimit(),
								credit.balanceCloseDate(), credit.balanceDueDate()));
	}

	static Transaction toTransaction(PluggyTransaction tx) {
		return new Transaction(tx.id(), tx.date(), tx.description(), tx.amount(), tx.currencyCode(),
				TransactionTypesEnum.fromPluggy(tx.type()), tx.status(), tx.category(),
				tx.creditCardMetadata() == null ? null : tx.creditCardMetadata().billId(),
				tx.amountInAccountCurrency());
	}

	static Bill toBill(PluggyBill bill) {
		return new Bill(bill.id(), bill.dueDate(), bill.billClosingDate(), bill.totalAmount(),
				bill.totalAmountCurrencyCode(), bill.minimumPaymentAmount());
	}

}
