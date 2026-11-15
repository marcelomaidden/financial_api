package com.mfaraujo.financialapi.resource.dto;

/** Seção {@code incomes} do resumo. */
public record IncomesSummary(SummarySection<IncomeAccount> available, SummarySection<IncomeTransaction> transactions) {
}
