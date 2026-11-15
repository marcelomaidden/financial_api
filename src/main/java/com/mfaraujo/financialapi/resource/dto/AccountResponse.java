package com.mfaraujo.financialapi.resource.dto;

import com.mfaraujo.financialapi.service.enums.AccountTypesEnum;

import java.math.BigDecimal;
import static com.mfaraujo.financialapi.service.enums.AccountTypesEnum.BANK;
import static com.mfaraujo.financialapi.service.enums.CurrencyEnum.BRL;

/** Conta exposta pela API. */
public record AccountResponse(
		String id,
		String itemId,
		AccountTypesEnum type,
		String subtype,
		String name,
		String number,
		BigDecimal balance,
		String currencyCode) {

	public boolean isBrlBankAccount() {
		return BANK.equals(type) && BRL.getType().equals(currencyCode) && balance != null;
	}
}
