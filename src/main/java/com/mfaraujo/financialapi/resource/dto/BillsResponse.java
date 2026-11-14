package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/** Página de faturas de uma conta de cartão. */
public record BillsResponse(
		String owner,
		String accountId,
		List<BillResponse> bills,
		Integer page,
		Integer totalPages) {
}
