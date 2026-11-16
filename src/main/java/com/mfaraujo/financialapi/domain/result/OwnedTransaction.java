package com.mfaraujo.financialapi.domain.result;

import com.mfaraujo.financialapi.domain.model.Transaction;

/** Transação com o dono e a conta de onde veio a consulta. */
public record OwnedTransaction(String owner, String accountId, Transaction transaction) {
}
