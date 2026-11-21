package com.mfaraujo.financialapi.resource.dto;

/**
 * Seção {@code expenses} do resumo. {@code accountDebits} são as saídas do período das
 * contas bancárias, separadas de {@code transactions} (compras de cartão sem fatura).
 */
public record ExpensesSummary(SummarySection<OwedBill> owed, SummarySection<ExpenseTransaction> transactions,
		SummarySection<AccountDebit> accountDebits) {

	public ExpensesSummary withAccountDebits(SummarySection<AccountDebit> accountDebits) {
		return new ExpensesSummary(owed, transactions, accountDebits);
	}

}
