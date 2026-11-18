package com.mfaraujo.financialapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyBillDetail;
import com.mfaraujo.financialapi.client.dto.PluggyLoan;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.LoanKindEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.BillDetail;
import com.mfaraujo.financialapi.domain.model.Loan;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import org.junit.jupiter.api.Test;

class PluggyMapperTest {

	@Test
	void mapsBankAccountWithoutCredit() {
		PluggyAccount source = new PluggyAccount("a1", "item1", "BANK", "CHECKING_ACCOUNT", "123", "Nubank", null,
				new BigDecimal("500.50"), "BRL", null);

		Account account = PluggyMapper.toAccount(source);

		assertThat(account.id()).isEqualTo("a1");
		assertThat(account.itemId()).isEqualTo("item1");
		assertThat(account.type()).isEqualTo(AccountTypesEnum.BANK);
		assertThat(account.subtype()).isEqualTo("CHECKING_ACCOUNT");
		assertThat(account.name()).isEqualTo("Nubank");
		assertThat(account.number()).isEqualTo("123");
		assertThat(account.balance()).isEqualByComparingTo("500.50");
		assertThat(account.currencyCode()).isEqualTo("BRL");
		assertThat(account.credit()).isNull();
	}

	@Test
	void mapsCreditCardWithCreditData() {
		PluggyAccount.CreditData credit = new PluggyAccount.CreditData("GOLD", "VISA", LocalDate.of(2026, 11, 5),
				LocalDate.of(2026, 11, 12), new BigDecimal("800"), new BigDecimal("1000"), new BigDecimal("50"),
				"ACTIVE");
		PluggyAccount source = new PluggyAccount("c1", "item1", "CREDIT", "CREDIT_CARD", "9999", "Cartão", null,
				new BigDecimal("200"), "BRL", credit);

		Account account = PluggyMapper.toAccount(source);

		assertThat(account.isCreditCard()).isTrue();
		assertThat(account.credit()).isEqualTo(new Account.Credit("VISA", new BigDecimal("1000"),
				new BigDecimal("800"), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)));
	}

	@Test
	void unknownAccountTypeBecomesUnknown() {
		PluggyAccount source = new PluggyAccount("a1", "i", "INVESTMENT", null, null, "x", null, BigDecimal.ONE,
				"BRL", null);

		assertThat(PluggyMapper.toAccount(source).type()).isEqualTo(AccountTypesEnum.UNKNOWN);
	}

	@Test
	void mapsTransactionAndUnknownType() {
		OffsetDateTime date = OffsetDateTime.parse("2026-11-10T12:00:00Z");
		PluggyTransaction source = new PluggyTransaction("t1", "a1", date, "PIX Recebido", new BigDecimal("19.99"),
				"BRL", "CREDIT", "POSTED", "Transfer", null, null, null);

		Transaction tx = PluggyMapper.toTransaction(source);

		assertThat(tx).isEqualTo(new Transaction("t1", date, "PIX Recebido", new BigDecimal("19.99"), "BRL",
				TransactionTypesEnum.CREDIT, "POSTED", "Transfer", null, null, null));
		assertThat(PluggyMapper.toTransaction(new PluggyTransaction("t2", "a1", date, "x", BigDecimal.ONE, "BRL",
				"REFUND", null, null, null, null, null)).type()).isEqualTo(TransactionTypesEnum.UNKNOWN);
	}

	@Test
	void mapsBillIdAndAmountInAccountCurrency() {
		OffsetDateTime date = OffsetDateTime.parse("2026-11-10T12:00:00Z");

		Transaction billed = PluggyMapper.toTransaction(new PluggyTransaction("t1", "c1", date, "Netflix",
				new BigDecimal("-58"), "USD", "DEBIT", "POSTED", null, null, new BigDecimal("-298.19"),
				new PluggyTransaction.CreditCardMetadata("bill-1", null, null)));
		Transaction unbilled = PluggyMapper.toTransaction(new PluggyTransaction("t2", "c1", date, "Mercado",
				new BigDecimal("30"), "BRL", "DEBIT", "PENDING", null, null, null,
				new PluggyTransaction.CreditCardMetadata(null, null, null)));
		Transaction noMetadata = PluggyMapper.toTransaction(new PluggyTransaction("t3", "c1", date, "Padaria",
				new BigDecimal("10"), "BRL", "DEBIT", "PENDING", null, null, null, null));

		assertThat(billed.billId()).isEqualTo("bill-1");
		assertThat(billed.amountInAccountCurrency()).isEqualByComparingTo("-298.19");
		assertThat(unbilled.billId()).isNull();
		assertThat(unbilled.amountInAccountCurrency()).isNull();
		assertThat(noMetadata.billId()).isNull();
	}

	@Test
	void mapsInstallmentOnlyWhenNumberAndTotalAreInformed() {
		Transaction complete = installmentTx(new PluggyTransaction.CreditCardMetadata("b1", 3, 10));
		Transaction onlyNumber = installmentTx(new PluggyTransaction.CreditCardMetadata(null, 3, null));
		Transaction onlyTotal = installmentTx(new PluggyTransaction.CreditCardMetadata(null, null, 10));
		Transaction noInstallment = installmentTx(new PluggyTransaction.CreditCardMetadata(null, null, null));
		Transaction noMetadata = installmentTx(null);

		assertThat(complete.installment()).isEqualTo(new Transaction.Installment(3, 10));
		assertThat(complete.billId()).isEqualTo("b1");
		assertThat(onlyNumber.installment()).isNull();
		assertThat(onlyTotal.installment()).isNull();
		assertThat(noInstallment.installment()).isNull();
		assertThat(noMetadata.installment()).isNull();
	}

	private static Transaction installmentTx(PluggyTransaction.CreditCardMetadata metadata) {
		return PluggyMapper.toTransaction(new PluggyTransaction("t", "c1", OffsetDateTime.parse("2026-11-10T12:00:00Z"),
				"Compra", new BigDecimal("100"), "BRL", "DEBIT", "PENDING", null, null, null, metadata));
	}

	@Test
	void mapsACompleteLoan() {
		PluggyLoan source = new PluggyLoan("l1", "item1", "0007", "Crédito Consignado", "CREDITO_PESSOAL_COM_CONSIGNACAO",
				"LOAN", new BigDecimal("50000"), "BRL", LocalDateTime.of(2022, 8, 1, 0, 0),
				LocalDateTime.of(2028, 1, 15, 0, 0), new PluggyLoan.Installments(120, 65, 55, 0),
				new PluggyLoan.Payments(new BigDecimal("25000"), List.of(
						release(false, LocalDateTime.of(2023, 7, 15, 0, 0), "680.50"))));

		Loan loan = PluggyMapper.toLoan(source);

		assertThat(loan.id()).isEqualTo("l1");
		assertThat(loan.itemId()).isEqualTo("item1");
		assertThat(loan.contractNumber()).isEqualTo("0007");
		assertThat(loan.productName()).isEqualTo("Crédito Consignado");
		assertThat(loan.kind()).isEqualTo(LoanKindEnum.LOAN);
		assertThat(loan.type()).isEqualTo("CREDITO_PESSOAL_COM_CONSIGNACAO");
		assertThat(loan.contractAmount()).isEqualByComparingTo("50000");
		assertThat(loan.currencyCode()).isEqualTo("BRL");
		assertThat(loan.contractDate()).isEqualTo(LocalDate.of(2022, 8, 1));
		assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2028, 1, 15));
		assertThat(loan.installments()).isEqualTo(new Loan.Installments(120, 65, 55, 0));
		assertThat(loan.outstandingBalance()).isEqualByComparingTo("25000");
		assertThat(loan.lastPayment()).isEqualTo(new Loan.Payment(LocalDate.of(2023, 7, 15), new BigDecimal("680.50")));
	}

	@Test
	void mapsALoanWithoutOptionalBlocks() {
		PluggyLoan source = new PluggyLoan("l2", "item1", null, "Cheque especial", null,
				"UNARRANGED_ACCOUNT_OVERDRAFT", null, "BRL", null, null, null, null);

		Loan loan = PluggyMapper.toLoan(source);

		assertThat(loan.kind()).isEqualTo(LoanKindEnum.UNARRANGED_ACCOUNT_OVERDRAFT);
		assertThat(loan.installments()).isNull();
		assertThat(loan.outstandingBalance()).isNull();
		assertThat(loan.lastPayment()).isNull();
		assertThat(loan.contractDate()).isNull();
		assertThat(loan.dueDate()).isNull();
		assertThat(loan.contractAmount()).isNull();
	}

	@Test
	void keepsPartialInstallmentsWithNullFields() {
		PluggyLoan source = loanWith(new PluggyLoan.Installments(24, null, null, null), null);

		assertThat(PluggyMapper.toLoan(source).installments()).isEqualTo(new Loan.Installments(24, null, null, null));
	}

	@Test
	void unknownKindBecomesUnknown() {
		PluggyLoan source = new PluggyLoan("l3", "i", null, "x", null, "LEASING", null, "BRL", null, null, null, null);

		assertThat(PluggyMapper.toLoan(source).kind()).isEqualTo(LoanKindEnum.UNKNOWN);
	}

	@Test
	void lastPaymentIsTheMostRecentRegularOne() {
		PluggyLoan source = loanWith(null, new PluggyLoan.Payments(BigDecimal.TEN, List.of(
				release(false, LocalDateTime.of(2023, 5, 15, 0, 0), "600"),
				release(false, LocalDateTime.of(2023, 7, 15, 0, 0), "680"),
				release(false, LocalDateTime.of(2023, 6, 15, 0, 0), "650"))));

		assertThat(PluggyMapper.toLoan(source).lastPayment())
				.isEqualTo(new Loan.Payment(LocalDate.of(2023, 7, 15), new BigDecimal("680")));
	}

	@Test
	void lastPaymentIgnoresAnOverParcelPayment() {
		PluggyLoan source = loanWith(null, new PluggyLoan.Payments(BigDecimal.TEN, List.of(
				release(false, LocalDateTime.of(2023, 7, 15, 0, 0), "680"),
				release(true, LocalDateTime.of(2023, 8, 1, 0, 0), "1000"))));

		assertThat(PluggyMapper.toLoan(source).lastPayment())
				.isEqualTo(new Loan.Payment(LocalDate.of(2023, 7, 15), new BigDecimal("680")));
	}

	@Test
	void lastPaymentIsNullWhenThereAreOnlyOverParcelOrIncompletePayments() {
		PluggyLoan onlyOverParcel = loanWith(null, new PluggyLoan.Payments(BigDecimal.TEN, List.of(
				release(true, LocalDateTime.of(2023, 8, 1, 0, 0), "1000"))));
		PluggyLoan incomplete = loanWith(null, new PluggyLoan.Payments(BigDecimal.TEN, List.of(
				new PluggyLoan.Release(false, null, new BigDecimal("1")),
				new PluggyLoan.Release(false, LocalDateTime.of(2023, 8, 1, 0, 0), null))));
		PluggyLoan noReleases = loanWith(null, new PluggyLoan.Payments(BigDecimal.TEN, null));

		assertThat(PluggyMapper.toLoan(onlyOverParcel).lastPayment()).isNull();
		assertThat(PluggyMapper.toLoan(incomplete).lastPayment()).isNull();
		assertThat(PluggyMapper.toLoan(noReleases).lastPayment()).isNull();
		assertThat(PluggyMapper.toLoan(noReleases).outstandingBalance()).isEqualByComparingTo("10");
	}

	@Test
	void treatsAMissingOverParcelFlagAsARegularPayment() {
		PluggyLoan source = loanWith(null, new PluggyLoan.Payments(BigDecimal.TEN, List.of(
				new PluggyLoan.Release(null, LocalDateTime.of(2023, 7, 15, 0, 0), new BigDecimal("680")))));

		assertThat(PluggyMapper.toLoan(source).lastPayment()).isNotNull();
	}

	private static PluggyLoan loanWith(PluggyLoan.Installments installments, PluggyLoan.Payments payments) {
		return new PluggyLoan("l", "i", null, "x", null, "LOAN", null, "BRL", null, null, installments, payments);
	}

	private static PluggyLoan.Release release(boolean overParcel, LocalDateTime paidDate, String amount) {
		return new PluggyLoan.Release(overParcel, paidDate, new BigDecimal(amount));
	}

	@Test
	void mapsBill() {
		LocalDateTime due = LocalDateTime.of(2026, 11, 12, 0, 0);
		LocalDateTime closing = LocalDateTime.of(2026, 11, 5, 0, 0);
		PluggyBill source = new PluggyBill("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30"));

		assertThat(PluggyMapper.toBill(source)).isEqualTo(
				new Bill("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30")));
	}

	private static PluggyBillDetail detail(LocalDateTime closing, BigDecimal minimum, Boolean allowsInstallments,
			List<PluggyBillDetail.FinanceCharge> charges, List<PluggyBillDetail.Payment> payments) {
		return new PluggyBillDetail("b1", LocalDateTime.of(2026, 11, 20, 0, 0), closing, new BigDecimal("1500"),
				"BRL", minimum, allowsInstallments, charges, payments);
	}

	@Test
	void mapsACompleteBillDetail() {
		PluggyBillDetail source = detail(LocalDateTime.of(2026, 11, 10, 0, 0), new BigDecimal("225"), true,
				List.of(new PluggyBillDetail.FinanceCharge("fc1", "IOF", new BigDecimal("12.5"), "BRL", "info")),
				List.of(new PluggyBillDetail.Payment("p1", "TOTAL", LocalDateTime.of(2026, 11, 18, 0, 0), "PIX",
						new BigDecimal("300"), "BRL")));

		BillDetail bill = PluggyMapper.toBillDetail(source);

		assertThat(bill.id()).isEqualTo("b1");
		assertThat(bill.dueDate()).isEqualTo(LocalDate.of(2026, 11, 20));
		assertThat(bill.closingDate()).isEqualTo(LocalDate.of(2026, 11, 10));
		assertThat(bill.totalAmount()).isEqualByComparingTo("1500");
		assertThat(bill.currencyCode()).isEqualTo("BRL");
		assertThat(bill.minimumPaymentAmount()).isEqualByComparingTo("225");
		assertThat(bill.allowsInstallments()).isTrue();
		assertThat(bill.financeCharges()).containsExactly(
				new BillDetail.FinanceCharge("fc1", "IOF", new BigDecimal("12.5"), "BRL", "info"));
		assertThat(bill.payments()).containsExactly(new BillDetail.Payment("p1", "TOTAL", LocalDate.of(2026, 11, 18),
				"PIX", new BigDecimal("300"), "BRL"));
	}

	@Test
	void mapsABillDetailWithoutClosingMinimumAndInstallmentsFlag() {
		BillDetail bill = PluggyMapper.toBillDetail(detail(null, null, null, List.of(), List.of()));

		assertThat(bill.closingDate()).isNull();
		assertThat(bill.minimumPaymentAmount()).isNull();
		assertThat(bill.allowsInstallments()).isNull();
	}

	@Test
	void mapsChargesAndPaymentsWithMissingOptionalFields() {
		BillDetail bill = PluggyMapper.toBillDetail(detail(null, null, null,
				List.of(new PluggyBillDetail.FinanceCharge("fc1", "JUROS", new BigDecimal("3"), "BRL", null)),
				List.of(new PluggyBillDetail.Payment("p1", "PARTIAL", null, null, new BigDecimal("50"), "BRL"))));

		assertThat(bill.financeCharges().get(0).additionalInfo()).isNull();
		assertThat(bill.payments().get(0).paymentDate()).isNull();
		assertThat(bill.payments().get(0).paymentMode()).isNull();
	}

	@Test
	void mapsNullChargesAndPaymentsToEmptyLists() {
		BillDetail bill = PluggyMapper.toBillDetail(detail(null, null, null, null, null));

		assertThat(bill.financeCharges()).isEmpty();
		assertThat(bill.payments()).isEmpty();
	}

	@Test
	void keepsTheDayOfDatesThatCarryATimeInBillDetail() {
		PluggyBillDetail source = new PluggyBillDetail("b1", LocalDateTime.of(2026, 11, 20, 23, 59),
				LocalDateTime.of(2026, 11, 10, 0, 0, 1), BigDecimal.ONE, "BRL", null, null, List.of(),
				List.of(new PluggyBillDetail.Payment("p1", "TOTAL", LocalDateTime.of(2026, 11, 18, 23, 30), null,
						BigDecimal.ONE, "BRL")));

		BillDetail bill = PluggyMapper.toBillDetail(source);

		assertThat(bill.dueDate()).isEqualTo(LocalDate.of(2026, 11, 20));
		assertThat(bill.closingDate()).isEqualTo(LocalDate.of(2026, 11, 10));
		assertThat(bill.payments().get(0).paymentDate()).isEqualTo(LocalDate.of(2026, 11, 18));
	}

}
