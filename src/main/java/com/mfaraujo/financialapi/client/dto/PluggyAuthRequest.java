package com.mfaraujo.financialapi.client.dto;

/** Corpo de {@code POST /auth}. */
public record PluggyAuthRequest(String clientId, String clientSecret) {

	/** Nunca expor o segredo em logs. */
	@Override
	public String toString() {
		return "PluggyAuthRequest[clientId=***, clientSecret=***]";
	}

}
