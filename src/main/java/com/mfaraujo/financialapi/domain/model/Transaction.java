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
		String category,
		String billId,
		BigDecimal amountInAccountCurrency) {

	static final String PENDING = "PENDING";

	/** Compra de cartão ainda fora de fatura: saída, pendente e sem {@code billId}. */
	public boolean isUnbilledPendingDebit() {
		return TransactionTypesEnum.DEBIT == type && PENDING.equals(status) && (billId == null || billId.isBlank());
	}

	/**
	 * Valor em BRL: o próprio {@code amount} quando a moeda é BRL, ou o
	 * {@code amountInAccountCurrency} nas demais. Nulo quando o valor não está disponível.
	 */
	public BigDecimal amountInBrl() {
		return CurrencyEnum.BRL.getType().equals(currencyCode) ? amount : amountInAccountCurrency;
	}

	/** Entrada de dinheiro em BRL e com valor informado. */
	public boolean isBrlCredit() {
		return TransactionTypesEnum.CREDIT == type && CurrencyEnum.BRL.getType().equals(currencyCode)
				&& amount != null;
	}

}
