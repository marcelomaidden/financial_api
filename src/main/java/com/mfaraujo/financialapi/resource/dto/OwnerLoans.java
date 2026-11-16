package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/**
 * Empréstimos de um dono. Em falha, {@code loans} é vazio e {@code error} traz o
 * motivo; em sucesso, {@code error} é nulo.
 */
public record OwnerLoans(String owner, List<LoanResponse> loans, ApiError error) {
}
