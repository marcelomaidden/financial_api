package com.mfaraujo.financialapi.domain.result;

import java.util.List;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.resource.dto.ApiError;

/** Contas de um dono. Em falha, {@code accounts} é vazio e {@code error} traz o motivo. */
public record OwnerAccountsResult(String owner, List<Account> accounts, ApiError error) {
}
