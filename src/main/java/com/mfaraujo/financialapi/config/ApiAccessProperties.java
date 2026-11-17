package com.mfaraujo.financialapi.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Quem pode chamar a API: a chave de API ({@code X-API-Key}) e as origens de navegador
 * permitidas por CORS. Chave em branco desliga a verificação, o que só vale em
 * desenvolvimento: em produção {@link ApiKeyRequired} impede a inicialização.
 */
@ConfigurationProperties("app.access")
public record ApiAccessProperties(@DefaultValue("") String apiKey, @DefaultValue Cors cors) {

	public ApiAccessProperties {
		apiKey = apiKey == null ? "" : apiKey;
		cors = cors == null ? new Cors(List.of()) : cors;
	}

	public boolean apiKeyEnabled() {
		return !apiKey.isBlank();
	}

	/** Nunca expor a chave em logs ou mensagens. */
	@Override
	public String toString() {
		return "ApiAccessProperties[apiKeyConfigured=" + apiKeyEnabled() + ", cors=" + cors + "]";
	}

	/** Origens com esquema e sem barra no final, por exemplo {@code https://front.exemplo.com}. */
	public record Cors(List<String> allowedOrigins) {

		public Cors {
			allowedOrigins = allowedOrigins == null ? List.of()
					: allowedOrigins.stream().filter(origin -> origin != null && !origin.isBlank())
							.map(String::trim).toList();
		}

	}

}
