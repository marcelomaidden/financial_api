package com.mfaraujo.financialapi.domain.result;

import java.util.List;

import com.mfaraujo.financialapi.domain.model.Transaction;

/** Página de transações de uma conta. {@code nextCursor} é nulo quando não há mais páginas. */
public record TransactionPage(List<Transaction> transactions, String nextCursor) {
}
