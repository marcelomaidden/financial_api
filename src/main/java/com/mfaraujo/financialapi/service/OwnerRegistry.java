package com.mfaraujo.financialapi.service;

import java.util.List;

import com.mfaraujo.financialapi.config.PluggyProperties;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;

import org.springframework.stereotype.Component;

/** Acesso aos donos configurados. */
@Component
public class OwnerRegistry {

	private final PluggyProperties properties;

	public OwnerRegistry(PluggyProperties properties) {
		this.properties = properties;
	}

	/** Todos os donos configurados. */
	public List<PluggyProperties.Owner> all() {
		return properties.owners();
	}

	/** Dono configurado com esse nome, ou {@link UnknownOwnerException} (404). */
	public PluggyProperties.Owner require(String name) {
		return properties.findOwner(name).orElseThrow(() -> new UnknownOwnerException(name));
	}

}
