package com.mfaraujo.financialapi.resource;

import java.util.List;
import java.util.function.Function;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.BillDetail;
import com.mfaraujo.financialapi.domain.model.Loan;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.AccountFailure;
import com.mfaraujo.financialapi.domain.result.BillDetailResult;
import com.mfaraujo.financialapi.domain.result.BillPage;
import com.mfaraujo.financialapi.domain.result.OwnedTransaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.OwnerCreditCardsResult;
import com.mfaraujo.financialapi.domain.result.OwnerLoansResult;
import com.mfaraujo.financialapi.domain.result.TransactionListResult;
import com.mfaraujo.financialapi.domain.result.TransactionPage;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.BillDetailResponse;
import com.mfaraujo.financialapi.resource.dto.BillResponse;
import com.mfaraujo.financialapi.resource.dto.BillsResponse;
import com.mfaraujo.financialapi.resource.dto.ConsolidatedTransaction;
import com.mfaraujo.financialapi.resource.dto.ConsolidatedTransactionsResponse;
import com.mfaraujo.financialapi.resource.dto.CreditCardResponse;
import com.mfaraujo.financialapi.resource.dto.Installment;
import com.mfaraujo.financialapi.resource.dto.LoanResponse;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
import com.mfaraujo.financialapi.resource.dto.OwnerLoans;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionError;
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

	static OwnersResponse<OwnerLoans> toLoans(OwnersResponse<OwnerLoansResult> result) {
		return map(result, ResponseMapper::toOwnerLoans);
	}

	static ConsolidatedTransactionsResponse toConsolidatedTransactions(TransactionListResult result) {
		List<TransactionError> errors = result.failures().stream().map(ResponseMapper::toTransactionError).toList();
		return new ConsolidatedTransactionsResponse(
				result.transactions().stream().map(ResponseMapper::toConsolidatedTransaction).toList(),
				!errors.isEmpty(), errors);
	}

	static ConsolidatedTransaction toConsolidatedTransaction(OwnedTransaction owned) {
		Transaction tx = owned.transaction();
		return new ConsolidatedTransaction(owned.owner(), owned.accountId(), tx.id(), tx.date(), tx.description(),
				tx.amount(), tx.currencyCode(), tx.type().getType(), tx.status(), tx.category());
	}

	static TransactionError toTransactionError(AccountFailure failure) {
		return new TransactionError(failure.owner(), failure.accountId(), failure.error().status(),
				failure.error().message());
	}

	static TransactionsResponse toTransactions(String owner, String accountId, TransactionPage page) {
		return new TransactionsResponse(owner, accountId,
				page.transactions().stream().map(ResponseMapper::toTransaction).toList(), page.nextCursor());
	}

	static BillsResponse toBills(String owner, String accountId, BillPage page) {
		return new BillsResponse(owner, accountId, page.bills().stream().map(ResponseMapper::toBill).toList(),
				page.page(), page.totalPages());
	}

	static BillDetailResponse toBillDetail(String owner, String accountId, BillDetailResult result) {
		BillDetail bill = result.bill();
		BillDetailResponse.BillSummary summary = new BillDetailResponse.BillSummary(bill.id(), bill.dueDate(),
				bill.closingDate(), bill.totalAmount(), bill.currencyCode(), bill.minimumPaymentAmount(),
				bill.allowsInstallments(),
				bill.financeCharges().stream()
						.map(charge -> new BillDetailResponse.FinanceChargeResponse(charge.id(), charge.type(),
								charge.amount(), charge.currencyCode(), charge.additionalInfo()))
						.toList(),
				bill.payments().stream()
						.map(payment -> new BillDetailResponse.PaymentResponse(payment.id(), payment.valueType(),
								payment.paymentDate(), payment.paymentMode(), payment.amount(),
								payment.currencyCode()))
						.toList());
		return new BillDetailResponse(owner, accountId, summary,
				result.transactions().stream().map(ResponseMapper::toBillTransaction).toList());
	}

	/** A data da compra vira o dia lido, sem conversão de fuso. */
	static BillDetailResponse.BillTransaction toBillTransaction(Transaction tx) {
		Transaction.Installment installment = tx.installment();
		return new BillDetailResponse.BillTransaction(tx.id(), tx.date() == null ? null : tx.date().toLocalDate(),
				tx.description(), tx.amount(), tx.currencyCode(), tx.category(), tx.type().getType(), tx.status(),
				installment == null ? null : new Installment(installment.number(), installment.total()));
	}

	static OwnerLoans toOwnerLoans(OwnerLoansResult result) {
		return new OwnerLoans(result.owner(), result.loans().stream().map(ResponseMapper::toLoan).toList(),
				result.error());
	}

	static LoanResponse toLoan(Loan loan) {
		Loan.Installments installments = loan.installments();
		Loan.Payment lastPayment = loan.lastPayment();
		return new LoanResponse(loan.id(), loan.itemId(), loan.contractNumber(), loan.productName(),
				loan.kind().getType(), loan.type(), loan.contractAmount(), loan.currencyCode(), loan.contractDate(),
				loan.dueDate(),
				installments == null ? null
						: new LoanResponse.Installments(installments.total(), installments.paid(),
								installments.remaining(), installments.pastDue()),
				loan.outstandingBalance(),
				lastPayment == null ? null
						: new LoanResponse.LastPayment(lastPayment.paidDate(), lastPayment.paidAmount()));
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
