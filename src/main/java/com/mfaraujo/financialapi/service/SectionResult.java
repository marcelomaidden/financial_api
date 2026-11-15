package com.mfaraujo.financialapi.service;

import java.util.List;

import com.mfaraujo.financialapi.resource.dto.SummaryError;

/** Uma seção do resumo e os erros que a consulta dela provocou. */
public record SectionResult<T>(T section, List<SummaryError> errors) {
}
