package com.mfaraujo.financialapi.service;

import java.time.LocalDate;

/**
 * Datas que a seção {@code expenses} usa. As faturas são filtradas pelo vencimento
 * ({@code dueFrom} a {@code dueTo}). As compras sem fatura não usam o período pedido:
 * a janela delas é a de cada cartão, calculada a partir de {@code today}.
 */
public record ExpensesPeriod(LocalDate dueFrom, LocalDate dueTo, LocalDate today) {
}
