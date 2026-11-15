package com.mfaraujo.financialapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.mfaraujo.financialapi.exception.AllOwnersFailedException;
import com.mfaraujo.financialapi.exception.ApiErrors;
import com.mfaraujo.financialapi.resource.dto.ApiError;
import com.mfaraujo.financialapi.resource.dto.OwnersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * Executa uma consulta para todos os donos, ou só para um quando informado.
 * <p>
 * Sem filtro, a falha de um dono vira erro na entrada dele e não descarta os
 * demais. Se todos falharem, lança {@link AllOwnersFailedException} (502). Com
 * filtro, a falha segue o fluxo normal de erro.
 */
@Component
public class OwnerFanOut {

	private static final Logger log = LoggerFactory.getLogger(OwnerFanOut.class);

	private final OwnerRegistry owners;

	public OwnerFanOut(OwnerRegistry owners) {
		this.owners = owners;
	}

	/**
	 * @param owner dono pedido, ou {@code null} para todos
	 * @param onSuccess monta a entrada de um dono a partir do nome dele
	 * @param onFailure monta a entrada de um dono que falhou
	 */
	public <E> OwnersResponse<E> collect(String owner, Function<String, E> onSuccess,
			BiFunction<String, ApiError, E> onFailure) {
		if (owner != null) {
			owners.require(owner);
			return new OwnersResponse<>(List.of(onSuccess.apply(owner)));
		}
		List<E> results = new ArrayList<>();
		int failures = 0;
		for (String name : owners.all().stream().map(o -> o.name()).toList()) {
			try {
				results.add(onSuccess.apply(name));
			}
			catch (RuntimeException ex) {
				failures++;
				results.add(onFailure.apply(name, logAndConvert(name, ex)));
			}
		}
		if (failures == results.size()) {
			throw new AllOwnersFailedException();
		}
		return new OwnersResponse<>(results);
	}

	/** Registra a falha de um dono e a converte no erro que vai na entrada dele. */
	private ApiError logAndConvert(String owner, RuntimeException ex) {
		ApiError error = ApiErrors.from(ex);
		log.warn("Consulta à Pluggy falhou para o dono '{}': {} {}", owner, error.status(), error.message());
		log.debug("Detalhe da falha do dono '{}'", owner, ex);
		return error;
	}

}
