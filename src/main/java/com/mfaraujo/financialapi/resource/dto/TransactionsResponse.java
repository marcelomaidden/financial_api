package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/**
 * Página de transações de uma conta. {@code nextCursor} vai no parâmetro
 * {@code after} para buscar a próxima página; é nulo quando não há mais.
 */
public record TransactionsResponse(
		String owner,
		String accountId,
		List<TransactionResponse> transactions,
		String nextCursor) {
}
