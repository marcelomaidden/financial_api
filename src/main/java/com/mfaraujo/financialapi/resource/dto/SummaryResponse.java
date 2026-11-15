package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/** Resposta de {@code GET /summary}. {@code partial} é verdadeiro quando {@code errors} não está vazio. */
public record SummaryResponse(IncomesSummary incomes, boolean partial, List<SummaryError> errors) {
}
