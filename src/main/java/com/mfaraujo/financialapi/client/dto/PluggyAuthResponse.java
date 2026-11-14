package com.mfaraujo.financialapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Resposta de {@code POST /auth}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyAuthResponse(String apiKey) {

	/** Nunca expor a chave em logs. */
	@Override
	public String toString() {
		return "PluggyAuthResponse[apiKey=***]";
	}

}
