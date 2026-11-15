package com.mfaraujo.financialapi.domain.result;

import java.util.List;

import com.mfaraujo.financialapi.domain.model.Account;
import com.mfaraujo.financialapi.resource.dto.ApiError;

/** Cartões de crédito de um dono. Em falha, {@code creditCards} é vazio e {@code error} traz o motivo. */
public record OwnerCreditCardsResult(String owner, List<Account> creditCards, ApiError error) {
}
