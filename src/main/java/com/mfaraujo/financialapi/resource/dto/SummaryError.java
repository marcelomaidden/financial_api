package com.mfaraujo.financialapi.resource.dto;

/** Falha de um dono no resumo. */
public record SummaryError(String owner, int status, String message) {
}
