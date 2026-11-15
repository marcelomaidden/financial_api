package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.mfaraujo.financialapi.domain.enums.CurrencyEnum;
import com.mfaraujo.financialapi.domain.enums.TransactionTypesEnum;
/** Transação de uma conta. */
public record Transaction(
		String id,
		OffsetDateTime date,
		String description,
		BigDecimal amount,
		String currencyCode,
		TransactionTypesEnum type,
		String status,
		String category) {

	/** Entrada de dinheiro em BRL e com valor informado. */
	public boolean isBrlCredit() {
		return TransactionTypesEnum.CREDIT == type && CurrencyEnum.BRL.getType().equals(currencyCode)
				&& amount != null;
	}

}
