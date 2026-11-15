package com.mfaraujo.financialapi.domain.result;

import java.util.List;

import com.mfaraujo.financialapi.domain.model.Bill;

/** Página de faturas de uma conta de cartão. */
public record BillPage(List<Bill> bills, Integer page, Integer totalPages) {
}
