package com.mfaraujo.financialapi.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta paginada por cursor ({@code GET /v2/transactions}). {@code next} é a
 * query string pronta para a próxima página, ou {@code null} se não houver mais.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyCursorPage<T>(List<T> results, String next) {

	public List<T> resultsOrEmpty() {
		return results == null ? List.of() : results;
	}

}
