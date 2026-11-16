package com.mfaraujo.financialapi.domain.result;

import com.mfaraujo.financialapi.resource.dto.ApiError;

/**
 * Falha de uma consulta. {@code accountId} é nulo quando a falha foi ao listar as
 * contas do dono.
 */
public record AccountFailure(String owner, String accountId, ApiError error) {
}
