package com.mfaraujo.financialapi.resource.dto;

/** Parcela de uma compra parcelada: a {@code number}-ésima de {@code total}. */
public record Installment(int number, int total) {
}
