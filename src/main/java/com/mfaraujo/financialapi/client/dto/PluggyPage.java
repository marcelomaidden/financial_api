package com.mfaraujo.financialapi.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Resposta paginada por número de página ({@code /accounts}, {@code /bills}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyPage<T>(Integer page, Integer total, Integer totalPages, List<T> results) {

	public List<T> resultsOrEmpty() {
		return results == null ? List.of() : results;
	}

}
