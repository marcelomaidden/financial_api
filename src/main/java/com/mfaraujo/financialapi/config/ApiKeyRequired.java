package com.mfaraujo.financialapi.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Em produção a aplicação não inicia sem chave de API, para nunca ficar aberta por
 * esquecimento. Cobre também a variável que existe mas está em branco.
 */
@Component
@Profile("prod")
class ApiKeyRequired {

	ApiKeyRequired(ApiAccessProperties properties) {
		if (!properties.apiKeyEnabled()) {
			throw new IllegalStateException("A variável de ambiente APP_API_KEY é obrigatória no perfil prod");
		}
	}

}
