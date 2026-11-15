package com.mfaraujo.financialapi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mfaraujo.financialapi.domain.enums.CurrencyEnum;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.ExpenseTransaction;
import com.mfaraujo.financialapi.resource.dto.ExpensesSummary;
import com.mfaraujo.financialapi.resource.dto.OwedBill;
import com.mfaraujo.financialapi.resource.dto.SummaryError;
import com.mfaraujo.financialapi.resource.dto.SummarySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Seção {@code expenses} do resumo, só com cartões de crédito em BRL.
 * <ul>
 * <li>{@code owed}: faturas em BRL com vencimento no período.</li>
 * <li>{@code transactions}: compras ainda sem fatura ({@code DEBIT}, {@code PENDING}
 * e sem {@code billId}).</li>
 * </ul>
 * Recebe apenas donos cuja consulta de contas teve sucesso. Falha nas faturas tira o
 * dono só de {@code owed}, e falha nas compras tira o dono só de {@code transactions}.
 */
@Service
public class ExpensesSummaryService {

	private static final Logger log = LoggerFactory.getLogger(ExpensesSummaryService.class);

	private final BillService billService;

	private final TransactionService transactionService;

	public ExpensesSummaryService(BillService billService, TransactionService transactionService) {
		this.billService = billService;
		this.transactionService = transactionService;
	}

	public SectionResult<ExpensesSummary> summarize(List<OwnerAccountsResult> owners, ExpensesPeriod period) {
		Map<String, List<OwedBill>> owedDetail = new LinkedHashMap<>();
		Map<String, List<ExpenseTransaction>> transactionsDetail = new LinkedHashMap<>();
		List<BigDecimal> purchaseAmounts = new ArrayList<>();
		List<SummaryError> errors = new ArrayList<>();

		for (OwnerAccountsResult owner : owners) {
			List<Account> cards = owner.accounts().stream().filter(Account::isBrlCreditCard).toList();
			try {
				owedDetail.put(owner.owner(), owedOf(owner.owner(), cards, period));
			}
			catch (RuntimeException ex) {
				errors.add(toError(owner.owner(), "faturas", ex));
			}
			try {
				List<Purchase> purchases = unbilledPurchasesOf(owner.owner(), cards, period);
				transactionsDetail.put(owner.owner(), purchases.stream().map(Purchase::toExpense).toList());
				purchases.forEach(p -> purchaseAmounts.add(p.transaction().amountInBrl()));
			}
			catch (RuntimeException ex) {
				errors.add(toError(owner.owner(), "compras do cartão", ex));
			}
		}

		var owedTotal = SummaryTotals.brl(owedDetail.values().stream().flatMap(List::stream)
				.map(OwedBill::totalAmount).toList());
		var transactionsTotal = SummaryTotals.brl(purchaseAmounts);
		return new SectionResult<>(new ExpensesSummary(new SummarySection<>(owedTotal, owedDetail),
				new SummarySection<>(transactionsTotal, transactionsDetail)), errors);
	}

	/** Faturas em BRL dos cartões com vencimento entre {@code dueFrom} e {@code dueTo}. */
	private List<OwedBill> owedOf(String owner, List<Account> cards, ExpensesPeriod period) {
		List<OwedBill> owed = new ArrayList<>();
		for (Account card : cards) {
			for (Bill bill : billService.getAllBills(owner, card.id())) {
				if (bill.isBrl() && dueWithin(bill, period)) {
					owed.add(new OwedBill(bill.id(), card.id(), card.name(), bill.dueDate(), bill.totalAmount(),
							bill.currencyCode()));
				}
			}
		}
		return owed;
	}

	private static boolean dueWithin(Bill bill, ExpensesPeriod period) {
		if (bill.dueDate() == null) {
			return false;
		}
		LocalDate due = bill.dueDate().toLocalDate();
		return !due.isBefore(period.dueFrom()) && !due.isAfter(period.dueTo());
	}

	/** Compras dos cartões ainda fora de fatura no período das compras. */
	private List<Purchase> unbilledPurchasesOf(String owner, List<Account> cards, ExpensesPeriod period) {
		List<Purchase> purchases = new ArrayList<>();
		for (Account card : cards) {
			for (Transaction tx : transactionService.getAllTransactions(owner, card.id(), period.transactionsFrom(),
					period.transactionsTo())) {
				if (tx.isUnbilledPendingDebit()) {
					purchases.add(new Purchase(card.id(), tx));
				}
			}
		}
		return purchases;
	}

	private static SummaryError toError(String owner, String what, RuntimeException ex) {
		ApiError error = ApiErrors.from(ex);
		log.warn("Consulta de {} falhou para o dono '{}': {} {}", what, owner, error.status(), error.message());
		log.debug("Detalhe da falha de {} do dono '{}'", what, owner, ex);
		return new SummaryError(owner, error.status(), error.message());
	}

	/** Compra de cartão e a conta de onde veio. */
	private record Purchase(String accountId, Transaction transaction) {

		ExpenseTransaction toExpense() {
			boolean brl = CurrencyEnum.BRL.getType().equals(transaction.currencyCode());
			return new ExpenseTransaction(transaction.id(), accountId, transaction.description(),
					transaction.currencyCode(), transaction.amount(), transaction.date(), transaction.category(),
					brl ? null : transaction.amountInAccountCurrency());
		}

	}

}
