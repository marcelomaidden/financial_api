package com.mfaraujo.financialapi.resource.dto;

/**
 * Falha de uma consulta em {@code GET /transactions}. {@code accountId} é nulo quando a
 * falha foi ao listar as contas do dono.
 */
public record TransactionError(String owner, String accountId, int status, String message) {
}
