package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/**
 * Contas de um dono. Em falha, {@code accounts} é vazio e {@code error} traz o
 * motivo; em sucesso, {@code error} é nulo.
 */
public record OwnerAccounts(String owner, List<AccountResponse> accounts, ApiError error) {
}
