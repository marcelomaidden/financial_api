package com.mfaraujo.financialapi.service;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyLoan;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.LoanKindEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Loan;
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
				tx.amountInAccountCurrency(), installmentOf(tx.creditCardMetadata()));
	}

	/** Parcela só quando a Pluggy informa o número e o total. */
	private static Transaction.Installment installmentOf(PluggyTransaction.CreditCardMetadata metadata) {
		if (metadata == null || metadata.installmentNumber() == null || metadata.totalInstallments() == null) {
			return null;
		}
		return new Transaction.Installment(metadata.installmentNumber(), metadata.totalInstallments());
	}

	static Loan toLoan(PluggyLoan loan) {
		PluggyLoan.Installments installments = loan.installments();
		PluggyLoan.Payments payments = loan.payments();
		return new Loan(loan.id(), loan.itemId(), loan.contractNumber(), loan.productName(),
				LoanKindEnum.fromPluggy(loan.kind()), loan.type(), loan.contractAmount(), loan.currencyCode(),
				dayOf(loan.contractDate()), dayOf(loan.dueDate()),
				installments == null ? null
						: new Loan.Installments(installments.totalNumberOfInstallments(), installments.paidInstallments(),
								installments.contractRemainingNumber(), installments.pastDueInstallments()),
				payments == null ? null : payments.contractOutstandingBalance(),
				payments == null ? null : lastRegularPayment(payments.releasesOrEmpty()));
	}

	/** Pagamento regular de maior data, ignorando o avulso e o que veio sem data ou valor. */
	private static Loan.Payment lastRegularPayment(List<PluggyLoan.Release> releases) {
		return releases.stream()
				.filter(release -> !Boolean.TRUE.equals(release.isOverParcelPayment()))
				.filter(release -> release.paidDate() != null && release.paidAmount() != null)
				.max(Comparator.comparing(PluggyLoan.Release::paidDate))
				.map(release -> new Loan.Payment(release.paidDate().toLocalDate(), release.paidAmount()))
				.orElse(null);
	}

	/** As datas chegam à meia-noite UTC: vale o dia lido, sem conversão de fuso. */
	private static LocalDate dayOf(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.toLocalDate();
	}

	static Bill toBill(PluggyBill bill) {
		return new Bill(bill.id(), bill.dueDate(), bill.billClosingDate(), bill.totalAmount(),
				bill.totalAmountCurrencyCode(), bill.minimumPaymentAmount());
	}

}
