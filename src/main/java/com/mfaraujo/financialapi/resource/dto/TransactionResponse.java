package com.mfaraujo.financialapi.resource.dto;

import com.mfaraujo.financialapi.service.enums.TransactionTypesEnum;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.mfaraujo.financialapi.service.enums.CurrencyEnum.BRL;
import static com.mfaraujo.financialapi.service.enums.TransactionTypesEnum.CREDIT;

/** Transação exposta pela API. */
public record TransactionResponse(
		String id,
		OffsetDateTime date,
		String description,
		BigDecimal amount,
		String currencyCode,
		TransactionTypesEnum type,
		String status,
		String category) {
	public boolean isBrazilianCredit() {
		return CREDIT.equals(type) && BRL.getType().equals(currencyCode) && amount != null;
	}
}
