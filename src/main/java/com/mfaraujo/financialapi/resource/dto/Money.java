package com.mfaraujo.financialapi.resource.dto;

import java.math.BigDecimal;

/** Valor com a moeda em que a Pluggy o devolveu. */
public record Money(BigDecimal value, String currencyCode) {
}
