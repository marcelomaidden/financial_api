package com.mfaraujo.financialapi.resource.dto;

import java.util.List;

/** Resposta de {@code GET /accounts} e {@code GET /credit-cards}: uma entrada por dono. */
public record OwnersResponse<T>(List<T> owners) {
}
