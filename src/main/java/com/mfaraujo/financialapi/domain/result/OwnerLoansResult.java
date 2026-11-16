package com.mfaraujo.financialapi.domain.result;

import java.util.List;

import com.mfaraujo.financialapi.domain.model.Loan;
import com.mfaraujo.financialapi.resource.dto.ApiError;

/** Empréstimos de um dono. Em falha, {@code loans} é vazio e {@code error} traz o motivo. */
public record OwnerLoansResult(String owner, List<Loan> loans, ApiError error) {
}
