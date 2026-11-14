package com.mfaraujo.financialapi.resource.dto;

/** Corpo padrão de erro da API. */
public record ApiError(int status, String error, String message) {
}
