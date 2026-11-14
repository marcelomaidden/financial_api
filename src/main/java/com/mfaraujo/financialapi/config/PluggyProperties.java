package com.mfaraujo.financialapi.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração da integração com a Pluggy. Cada dono tem a própria conta de
 * desenvolvedor na Pluggy e, portanto, credenciais e itens próprios.
 */
@Validated
@ConfigurationProperties("pluggy")
public record PluggyProperties(
		@NotBlank @DefaultValue("https://api.pluggy.ai") String baseUrl,
		@NotNull @DefaultValue("5s") Duration connectTimeout,
		@NotNull @DefaultValue("30s") Duration readTimeout,
		@NotEmpty List<@Valid @NotNull Owner> owners) {

	@AssertTrue(message = "os nomes dos donos devem ser únicos")
	public boolean isOwnerNamesUnique() {
		if (owners == null) {
			return true;
		}
		var names = new HashSet<String>();
		return owners.stream()
				.filter(owner -> owner != null && owner.name() != null)
				.allMatch(owner -> names.add(owner.name()));
	}

	public Optional<Owner> findOwner(String name) {
		return owners.stream().filter(owner -> owner.name().equals(name)).findFirst();
	}

	public record Owner(
			@NotBlank String name,
			@NotBlank String clientId,
			@NotBlank String clientSecret,
			@NotEmpty List<@NotBlank String> itemIds) {

		/** Nunca expor credenciais em logs ou mensagens. */
		@Override
		public String toString() {
			return "Owner[name=" + name + ", itemIds=" + itemIds + "]";
		}
	}

}
