package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/**
 * Cartões de crédito de um dono. Em falha, {@code creditCards} é vazio e
 * {@code error} traz o motivo; em sucesso, {@code error} é nulo.
 */
public record OwnerCreditCards(String owner, List<CreditCardResponse> creditCards, ApiError error) {
}
