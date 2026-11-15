package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;

/** Conta bancária no detalhe de {@code incomes.available}. */
public record IncomeAccount(String id, String name, BigDecimal balance, String currencyCode) {
}
