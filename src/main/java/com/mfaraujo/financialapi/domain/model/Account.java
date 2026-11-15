package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.mfaraujo.financialapi.domain.enums.AccountTypesEnum;
import com.mfaraujo.financialapi.domain.enums.CurrencyEnum;
/**
 * Conta de um dono. O {@code id} vale só para o dono que o devolveu. {@code credit}
 * só vem preenchido em cartão de crédito e pode ser nulo quando a Pluggy não informa.
 */
public record Account(
		String id,
		String itemId,
		AccountTypesEnum type,
		String subtype,
		String name,
		String number,
		BigDecimal balance,
		String currencyCode,
		Credit credit) {

	public boolean isCreditCard() {
		return AccountTypesEnum.CREDIT == type;
	}

	/** Conta bancária em BRL e com saldo informado. */
	public boolean isBrlBankAccount() {
		return AccountTypesEnum.BANK == type && CurrencyEnum.BRL.getType().equals(currencyCode) && balance != null;
	}

	public record Credit(
			String brand,
			BigDecimal creditLimit,
			BigDecimal availableCreditLimit,
			LocalDate balanceCloseDate,
			LocalDate balanceDueDate) {
	}

}
