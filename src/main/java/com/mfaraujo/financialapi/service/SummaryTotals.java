package com.mfaraujo.financialapi.service;

import static com.mfaraujo.financialapi.domain.enums.CurrencyEnum.BRL;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

import com.mfaraujo.financialapi.resource.dto.Money;

/** Soma de valores em BRL para os totais das seções do resumo. */
final class SummaryTotals {

	private SummaryTotals() {
	}

	/** Soma os valores informados, ignorando os nulos. */
	static Money brl(Collection<BigDecimal> values) {
		BigDecimal sum = values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Money(sum, BRL.getType());
	}

}
