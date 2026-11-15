package com.mfaraujo.financialapi.resource.dto;

/** Seção {@code expenses} do resumo. */
public record ExpensesSummary(SummarySection<OwedBill> owed, SummarySection<ExpenseTransaction> transactions) {
}
