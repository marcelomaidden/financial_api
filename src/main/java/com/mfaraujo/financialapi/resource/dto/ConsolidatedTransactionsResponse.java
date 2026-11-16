package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/** Resposta de {@code GET /transactions}. {@code partial} é verdadeiro quando {@code errors} não está vazio. */
public record ConsolidatedTransactionsResponse(List<ConsolidatedTransaction> transactions, boolean partial,
		List<TransactionError> errors) {
}
