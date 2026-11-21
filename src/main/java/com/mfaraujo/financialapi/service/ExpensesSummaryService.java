package com.mfaraujo.financialapi.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mfaraujo.financialapi.domain.enums.CurrencyEnum;
import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.BillingCycle;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.ExpenseTransaction;
import com.mfaraujo.financialapi.resource.dto.ExpensesSummary;
import com.mfaraujo.financialapi.resource.dto.Installment;
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
 * As compras de cada cartão são buscadas pela data em que chegaram à Pluggy, numa
 * janela derivada da fatura mais recente do cartão ({@link BillingCycle}). Sem fatura,
 * ou com a consulta de faturas falhando, vale a janela padrão.
 * <p>
 * Recebe apenas donos cuja consulta de contas teve sucesso. Falha nas faturas tira o
 * dono só de {@code owed}, e falha nas compras tira o dono só de {@code transactions}.
 */
@Service
public class ExpensesSummaryService {

	/** Dias antes do fechamento anterior em que a janela de chegada das compras começa. */
	static final int LOWER_SLACK_DAYS = 10;

	/** Dias depois do vencimento da fatura aberta em que a data da compra ainda vale (vencimento em fim de semana). */
	static final int UPPER_SLACK_DAYS = 5;

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

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
			Map<String, List<Bill>> billsByCard = new LinkedHashMap<>();
			RuntimeException billsFailure = null;
			for (Account card : cards) {
				try {
					billsByCard.put(card.id(), billService.getAllBills(owner.owner(), card.id()));
				}
				catch (RuntimeException ex) {
					billsFailure = billsFailure == null ? ex : billsFailure;
				}
			}
			if (billsFailure == null) {
				owedDetail.put(owner.owner(), owedOf(cards, billsByCard, period));
			}
			else {
				errors.add(toError(owner.owner(), "faturas", billsFailure));
			}
			try {
				List<Purchase> purchases = unbilledPurchasesOf(owner.owner(), cards, billsByCard, period);
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
				new SummarySection<>(transactionsTotal, transactionsDetail),
				new SummarySection<>(SummaryTotals.brl(List.of()), Map.of())), errors);
	}

	/** Faturas em BRL dos cartões com vencimento entre {@code dueFrom} e {@code dueTo}. */
	private List<OwedBill> owedOf(List<Account> cards, Map<String, List<Bill>> billsByCard, ExpensesPeriod period) {
		List<OwedBill> owed = new ArrayList<>();
		for (Account card : cards) {
			for (Bill bill : billsByCard.get(card.id())) {
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

	/** Compras dos cartões ainda fora de fatura, na janela de cada cartão. */
	private List<Purchase> unbilledPurchasesOf(String owner, List<Account> cards, Map<String, List<Bill>> billsByCard,
			ExpensesPeriod period) {
		List<Purchase> purchases = new ArrayList<>();
		for (Account card : cards) {
			Window window = windowOf(billsByCard.getOrDefault(card.id(), List.of()), period.today());
			for (Transaction tx : transactionService.getAllCreatedSince(owner, card.id(), window.arrivedFrom())) {
				if (tx.isUnbilledPendingDebit() && !dateAfter(tx, window.dateUntil())) {
					purchases.add(new Purchase(card.id(), tx));
				}
			}
		}
		return purchases;
	}

	/**
	 * Janela do cartão. Com ciclo: chegou a partir do fechamento anterior menos
	 * {@value #LOWER_SLACK_DAYS} dias, e {@code date} até o vencimento da fatura aberta mais
	 * {@value #UPPER_SLACK_DAYS} dias, o que deixa de fora as parcelas das faturas seguintes.
	 * Sem ciclo: chegou a partir do dia 1 do mês anterior, e {@code date} até hoje.
	 */
	private static Window windowOf(List<Bill> bills, LocalDate today) {
		Optional<BillingCycle> cycle = BillingCycle.from(bills, today);
		if (cycle.isPresent()) {
			return new Window(cycle.get().previousClosing().minusDays(LOWER_SLACK_DAYS).atStartOfDay(ZONE).toInstant(),
					cycle.get().nextDue().plusDays(UPPER_SLACK_DAYS));
		}
		return new Window(today.withDayOfMonth(1).minusMonths(1).atStartOfDay(ZONE).toInstant(), today);
	}

	/** Compara só o dia da compra, sem converter fuso. Compra sem data não é descartada. */
	private static boolean dateAfter(Transaction tx, LocalDate limit) {
		return tx.date() != null && tx.date().toLocalDate().isAfter(limit);
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
					brl ? null : transaction.amountInAccountCurrency(), installmentOf(transaction));
		}

		private static Installment installmentOf(Transaction transaction) {
			Transaction.Installment installment = transaction.installment();
			return installment == null ? null : new Installment(installment.number(), installment.total());
		}

	}

	/** Janela de compras de um cartão: chegada a partir de {@code arrivedFrom} e {@code date} até {@code dateUntil}. */
	private record Window(Instant arrivedFrom, LocalDate dateUntil) {
	}

}
