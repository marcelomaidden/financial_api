package com.mfaraujo.financialapi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mfaraujo.financialapi.domain.enums.CurrencyEnum;

/** Fatura de cartão. As datas vêm sem fuso. */
public record Bill(
		String id,
		LocalDateTime dueDate,
		LocalDateTime closingDate,
		BigDecimal totalAmount,
		String currencyCode,
		BigDecimal minimumPaymentAmount) {

	/** Fatura em BRL e com valor informado. */
	public boolean isBrl() {
		return CurrencyEnum.BRL.getType().equals(currencyCode) && totalAmount != null;
	}

}
