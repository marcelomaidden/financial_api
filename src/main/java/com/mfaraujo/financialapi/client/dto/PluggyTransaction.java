package com.mfaraujo.financialapi.client.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mfaraujo.financialapi.service.enums.TransactionTypesEnum;

/** Transação da Pluggy ({@code GET /v2/transactions}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyTransaction(
		String id,
		String accountId,
		OffsetDateTime date,
		String description,
		BigDecimal amount,
		String currencyCode,
		TransactionTypesEnum type,
		String status,
		String category,
		BigDecimal balance) {
}
