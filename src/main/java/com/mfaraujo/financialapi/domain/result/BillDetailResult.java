package com.mfaraujo.financialapi.domain.result;

import java.util.List;

import com.mfaraujo.financialapi.domain.model.BillDetail;
import com.mfaraujo.financialapi.domain.model.Transaction;

/** Fatura de cartão com as compras dela, da mais recente para a mais antiga. */
public record BillDetailResult(BillDetail bill, List<Transaction> transactions) {
}
