package com.mfaraujo.financialapi.resource.dto;

import java.util.List;
import java.util.Map;

/** Parte de {@code incomes}: total e detalhe por dono. */
public record SummarySection<T>(Money total, Map<String, List<T>> detail) {
}
