package com.mfaraujo.financialapi.domain.result;

import java.util.List;

/** Transações de várias contas, já ordenadas, e as consultas que falharam. */
public record TransactionListResult(List<OwnedTransaction> transactions, List<AccountFailure> failures) {
}
