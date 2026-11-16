package com.mfaraujo.financialapi.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.LoanKindEnum;
import com.mfaraujo.financialapi.domain.model.Bill;
import com.mfaraujo.financialapi.domain.model.Loan;
import com.mfaraujo.financialapi.domain.model.Transaction;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
import com.mfaraujo.financialapi.domain.result.AccountFailure;
import com.mfaraujo.financialapi.domain.result.BillPage;
import com.mfaraujo.financialapi.domain.result.OwnedTransaction;
import com.mfaraujo.financialapi.domain.result.OwnerAccountsResult;
import com.mfaraujo.financialapi.domain.result.OwnerCreditCardsResult;
import com.mfaraujo.financialapi.domain.result.OwnerLoansResult;
import com.mfaraujo.financialapi.domain.result.TransactionListResult;
import com.mfaraujo.financialapi.domain.result.TransactionPage;
import com.mfaraujo.financialapi.resource.dto.AccountResponse;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.BillResponse;
import com.mfaraujo.financialapi.resource.dto.BillsResponse;
import com.mfaraujo.financialapi.resource.dto.ConsolidatedTransaction;
import com.mfaraujo.financialapi.resource.dto.ConsolidatedTransactionsResponse;
import com.mfaraujo.financialapi.resource.dto.CreditCardResponse;
import com.mfaraujo.financialapi.resource.dto.LoanResponse;
import com.mfaraujo.financialapi.resource.dto.OwnerAccounts;
import com.mfaraujo.financialapi.resource.dto.OwnerCreditCards;
import com.mfaraujo.financialapi.resource.dto.OwnerLoans;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionError;
import com.mfaraujo.financialapi.resource.dto.TransactionResponse;
import com.mfaraujo.financialapi.resource.dto.TransactionsResponse;
import org.junit.jupiter.api.Test;

class ResponseMapperTest {

	private static final OffsetDateTime DATE = OffsetDateTime.parse("2026-11-10T12:00:00Z");

	@Test
	void mapsAccountWithTypeAsText() {
		Account account = new Account("a1", "item1", AccountTypesEnum.BANK, "CHECKING_ACCOUNT", "Nubank", "123",
				new BigDecimal("500"), "BRL", null);

		assertThat(ResponseMapper.toAccount(account)).isEqualTo(new AccountResponse("a1", "item1", "BANK",
				"CHECKING_ACCOUNT", "Nubank", "123", new BigDecimal("500"), "BRL"));
	}

	@Test
	void unknownTypesAreExposedAsUnknown() {
		Account account = new Account("a1", "i", AccountTypesEnum.UNKNOWN, null, "x", null, BigDecimal.ONE, "BRL",
				null);
		Transaction tx = new Transaction("t", DATE, "x", BigDecimal.ONE, "BRL", TransactionTypesEnum.UNKNOWN, null,
				null, null, null, null);

		assertThat(ResponseMapper.toAccount(account).type()).isEqualTo("UNKNOWN");
		assertThat(ResponseMapper.toTransaction(tx).type()).isEqualTo("UNKNOWN");
	}

	@Test
	void mapsCreditCardWithAndWithoutCreditData() {
		Account.Credit credit = new Account.Credit("VISA", new BigDecimal("1000"), new BigDecimal("800"),
				LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
		Account withCredit = new Account("c1", "item1", AccountTypesEnum.CREDIT, null, "Cartão", "9999",
				new BigDecimal("200"), "BRL", credit);
		Account withoutCredit = new Account("c2", "item1", AccountTypesEnum.CREDIT, null, "Cartão 2", "8888",
				new BigDecimal("10"), "BRL", null);

		assertThat(ResponseMapper.toCreditCard(withCredit)).isEqualTo(new CreditCardResponse("c1", "item1", "Cartão",
				"9999", "VISA", new BigDecimal("200"), "BRL", new BigDecimal("1000"), new BigDecimal("800"),
				LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)));
		assertThat(ResponseMapper.toCreditCard(withoutCredit)).isEqualTo(new CreditCardResponse("c2", "item1",
				"Cartão 2", "8888", null, new BigDecimal("10"), "BRL", null, null, null, null));
	}

	@Test
	void mapsTransactionsPage() {
		Transaction tx = new Transaction("t1", DATE, "PIX", new BigDecimal("19.99"), "BRL",
				TransactionTypesEnum.CREDIT, "POSTED", "Transfer", null, null, null);

		TransactionsResponse response = ResponseMapper.toTransactions("marcelo", "a1",
				new TransactionPage(List.of(tx), "cursor"));

		assertThat(response.owner()).isEqualTo("marcelo");
		assertThat(response.accountId()).isEqualTo("a1");
		assertThat(response.nextCursor()).isEqualTo("cursor");
		assertThat(response.transactions()).containsExactly(new TransactionResponse("t1", DATE, "PIX",
				new BigDecimal("19.99"), "BRL", "CREDIT", "POSTED", "Transfer"));
	}

	@Test
	void mapsBillsPage() {
		LocalDateTime due = LocalDateTime.of(2026, 11, 12, 0, 0);
		LocalDateTime closing = LocalDateTime.of(2026, 11, 5, 0, 0);
		Bill bill = new Bill("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30"));

		BillsResponse response = ResponseMapper.toBills("luana", "c1", new BillPage(List.of(bill), 1, 3));

		assertThat(response.owner()).isEqualTo("luana");
		assertThat(response.accountId()).isEqualTo("c1");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.bills()).containsExactly(
				new BillResponse("b1", due, closing, new BigDecimal("300"), "BRL", new BigDecimal("30")));
	}

	@Test
	void keepsOwnerErrorInAccountsAndCreditCards() {
		ApiError error = new ApiError(502, "Bad Gateway", "falhou");
		Account account = new Account("a1", "i", AccountTypesEnum.BANK, null, "x", null, BigDecimal.ONE, "BRL", null);

		OwnersResponse<OwnerAccounts> accounts = ResponseMapper.toAccounts(new OwnersResponse<>(List.of(
				new OwnerAccountsResult("marcelo", List.of(account), null),
				new OwnerAccountsResult("luana", List.of(), error))));
		OwnersResponse<OwnerCreditCards> cards = ResponseMapper.toCreditCards(new OwnersResponse<>(List.of(
				new OwnerCreditCardsResult("luana", List.of(), error))));

		assertThat(accounts.owners()).extracting(OwnerAccounts::owner).containsExactly("marcelo", "luana");
		assertThat(accounts.owners().get(0).accounts()).hasSize(1);
		assertThat(accounts.owners().get(0).error()).isNull();
		assertThat(accounts.owners().get(1).error()).isEqualTo(error);
		assertThat(cards.owners().get(0).creditCards()).isEmpty();
		assertThat(cards.owners().get(0).error()).isEqualTo(error);
	}

	@Test
	void mapsACompleteLoan() {
		Loan loan = new Loan("l1", "item1", "0007", "Crédito Consignado", LoanKindEnum.LOAN,
				"CREDITO_PESSOAL_COM_CONSIGNACAO", new BigDecimal("50000"), "BRL", LocalDate.of(2022, 8, 1),
				LocalDate.of(2028, 1, 15), new Loan.Installments(120, 65, 55, 0), new BigDecimal("25000"),
				new Loan.Payment(LocalDate.of(2023, 7, 15), new BigDecimal("680.50")));

		LoanResponse response = ResponseMapper.toLoan(loan);

		assertThat(response).isEqualTo(new LoanResponse("l1", "item1", "0007", "Crédito Consignado", "LOAN",
				"CREDITO_PESSOAL_COM_CONSIGNACAO", new BigDecimal("50000"), "BRL", LocalDate.of(2022, 8, 1),
				LocalDate.of(2028, 1, 15), new LoanResponse.Installments(120, 65, 55, 0), new BigDecimal("25000"),
				new LoanResponse.LastPayment(LocalDate.of(2023, 7, 15), new BigDecimal("680.50"))));
	}

	@Test
	void mapsALoanWithNullFieldsAndKindAsText() {
		Loan loan = new Loan("l2", "item1", null, "Cheque especial", LoanKindEnum.UNKNOWN, null, null, "BRL", null,
				null, null, null, null);

		LoanResponse response = ResponseMapper.toLoan(loan);

		assertThat(response.kind()).isEqualTo("UNKNOWN");
		assertThat(response.installments()).isNull();
		assertThat(response.lastPayment()).isNull();
		assertThat(response.outstandingBalance()).isNull();
		assertThat(response.contractAmount()).isNull();
	}

	@Test
	void keepsOwnerErrorInLoans() {
		ApiError error = new ApiError(502, "Bad Gateway", "falhou");
		Loan loan = new Loan("l1", "i", null, "x", LoanKindEnum.FINANCING, null, null, "BRL", null, null, null, null,
				null);

		OwnersResponse<OwnerLoans> loans = ResponseMapper.toLoans(new OwnersResponse<>(List.of(
				new OwnerLoansResult("marcelo", List.of(loan), null),
				new OwnerLoansResult("luana", List.of(), error))));

		assertThat(loans.owners()).extracting(OwnerLoans::owner).containsExactly("marcelo", "luana");
		assertThat(loans.owners().get(0).loans()).extracting(LoanResponse::kind).containsExactly("FINANCING");
		assertThat(loans.owners().get(0).error()).isNull();
		assertThat(loans.owners().get(1).loans()).isEmpty();
		assertThat(loans.owners().get(1).error()).isEqualTo(error);
	}

	@Test
	void mapsTheConsolidatedTransactionsWithOwnerAndAccount() {
		Transaction tx = new Transaction("t1", DATE, "PIX", new BigDecimal("19.99"), "BRL", TransactionTypesEnum.CREDIT,
				"POSTED", "Transfer", null, null, null);

		ConsolidatedTransactionsResponse response = ResponseMapper.toConsolidatedTransactions(
				new TransactionListResult(List.of(new OwnedTransaction("marcelo", "a1", tx)), List.of()));

		assertThat(response.partial()).isFalse();
		assertThat(response.errors()).isEmpty();
		assertThat(response.transactions()).containsExactly(new ConsolidatedTransaction("marcelo", "a1", "t1", DATE,
				"PIX", new BigDecimal("19.99"), "BRL", "CREDIT", "POSTED", "Transfer"));
	}

	@Test
	void mapsFailuresToErrorsAndMarksTheResponseAsPartial() {
		ApiError accountError = new ApiError(429, "Too Many Requests", "Limite de requisições da Pluggy atingido");
		ApiError ownerError = new ApiError(502, "Bad Gateway", "Erro ao consultar a Pluggy");

		ConsolidatedTransactionsResponse response = ResponseMapper.toConsolidatedTransactions(new TransactionListResult(
				List.of(), List.of(new AccountFailure("marcelo", "a1", accountError),
						new AccountFailure("luana", null, ownerError))));

		assertThat(response.partial()).isTrue();
		assertThat(response.errors()).containsExactly(
				new TransactionError("marcelo", "a1", 429, "Limite de requisições da Pluggy atingido"),
				new TransactionError("luana", null, 502, "Erro ao consultar a Pluggy"));
	}

	@Test
	void unknownTransactionTypeIsExposedAsUnknownInTheConsolidatedList() {
		Transaction tx = new Transaction("t", DATE, "x", BigDecimal.ONE, "BRL", TransactionTypesEnum.UNKNOWN, null,
				null, null, null, null);

		ConsolidatedTransaction mapped = ResponseMapper.toConsolidatedTransaction(new OwnedTransaction("m", "a", tx));

		assertThat(mapped.type()).isEqualTo("UNKNOWN");
	}

}
