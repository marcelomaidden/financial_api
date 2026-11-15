package com.mfaraujo.financialapi.service;

import java.time.LocalDate;

/**
 * Datas que a seção {@code expenses} usa. As faturas são filtradas pelo vencimento
 * ({@code dueFrom} a {@code dueTo}) e as compras sem fatura pela data da compra
 * ({@code transactionsFrom} a {@code transactionsTo}).
 */
public record ExpensesPeriod(LocalDate dueFrom, LocalDate dueTo, LocalDate transactionsFrom,
		LocalDate transactionsTo) {
}
