package com.mfaraujo.financialapi.resource;

import java.util.List;
import java.util.function.Function;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.BillPage;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.OwnerCreditCardsResult;
import com.mfaraujo.financialapi.domain.result.TransactionPage;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.BillResponse;
import com.mfaraujo.financialapi.resource.dto.BillsResponse;
import com.mfaraujo.financialapi.resource.dto.CreditCardResponse;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;

/** Traduz o modelo de domínio para o contrato da API. */
final class ResponseMapper {

	private ResponseMapper() {
	}

	static OwnersResponse<OwnerAccounts> toAccounts(OwnersResponse<OwnerAccountsResult> result) {
		return map(result, ResponseMapper::toOwnerAccounts);
	}

	static OwnersResponse<OwnerCreditCards> toCreditCards(OwnersResponse<OwnerCreditCardsResult> result) {
		return map(result, ResponseMapper::toOwnerCreditCards);
	}

	static TransactionsResponse toTransactions(String owner, String accountId, TransactionPage page) {
		return new TransactionsResponse(owner, accountId,
				page.transactions().stream().map(ResponseMapper::toTransaction).toList(), page.nextCursor());
	}

	static BillsResponse toBills(String owner, String accountId, BillPage page) {
		return new BillsResponse(owner, accountId, page.bills().stream().map(ResponseMapper::toBill).toList(),
				page.page(), page.totalPages());
	}

	static OwnerAccounts toOwnerAccounts(OwnerAccountsResult result) {
		return new OwnerAccounts(result.owner(), result.accounts().stream().map(ResponseMapper::toAccount).toList(),
				result.error());
	}

	static OwnerCreditCards toOwnerCreditCards(OwnerCreditCardsResult result) {
		return new OwnerCreditCards(result.owner(),
				result.creditCards().stream().map(ResponseMapper::toCreditCard).toList(), result.error());
	}

	static AccountResponse toAccount(Account account) {
		return new AccountResponse(account.id(), account.itemId(), account.type().getType(), account.subtype(),
				account.name(), account.number(), account.balance(), account.currencyCode());
	}

	static CreditCardResponse toCreditCard(Account account) {
		Account.Credit credit = account.credit();
		return new CreditCardResponse(account.id(), account.itemId(), account.name(), account.number(),
				credit == null ? null : credit.brand(), account.balance(), account.currencyCode(),
				credit == null ? null : credit.creditLimit(),
				credit == null ? null : credit.availableCreditLimit(),
				credit == null ? null : credit.balanceCloseDate(),
				credit == null ? null : credit.balanceDueDate());
	}

	static TransactionResponse toTransaction(Transaction tx) {
		return new TransactionResponse(tx.id(), tx.date(), tx.description(), tx.amount(), tx.currencyCode(),
				tx.type().getType(), tx.status(), tx.category());
	}

	static BillResponse toBill(Bill bill) {
		return new BillResponse(bill.id(), bill.dueDate(), bill.closingDate(), bill.totalAmount(),
				bill.currencyCode(), bill.minimumPaymentAmount());
	}

	private static <S, T> OwnersResponse<T> map(OwnersResponse<S> source, Function<S, T> mapper) {
		List<T> owners = source.owners().stream().map(mapper).toList();
		return new OwnersResponse<>(owners);
	}

}
