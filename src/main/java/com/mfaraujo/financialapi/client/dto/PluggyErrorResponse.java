package com.mfaraujo.financialapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Corpo de erro da Pluggy. Os campos variam por endpoint; todos são opcionais. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyErrorResponse(Integer code, String message, String errorMessage) {

	public String bestMessage() {
		if (message != null && !message.isBlank()) {
			return message;
		}
		return errorMessage;
	}

}
