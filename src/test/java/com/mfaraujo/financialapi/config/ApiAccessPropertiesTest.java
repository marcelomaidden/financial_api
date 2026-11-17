package com.mfaraujo.financialapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ApiAccessPropertiesTest {

	private static ApiAccessProperties bind(Map<String, Object> source) {
		return new Binder(new MapConfigurationPropertySource(source))
				.bind("app.access", Bindable.of(ApiAccessProperties.class))
				.orElseGet(() -> new ApiAccessProperties(null, null));
	}

	@Test
	void bindsACommaSeparatedListOfOrigins() {
		ApiAccessProperties properties = bind(Map.of("app.access.api-key", "segredo",
				"app.access.cors.allowed-origins", "https://a.example.com,https://b.example.com"));

		assertThat(properties.cors().allowedOrigins()).containsExactly("https://a.example.com",
				"https://b.example.com");
		assertThat(properties.apiKey()).isEqualTo("segredo");
		assertThat(properties.apiKeyEnabled()).isTrue();
	}

	@Test
	void trimsSpacesAndIgnoresBlankOrigins() {
		ApiAccessProperties properties = bind(
				Map.of("app.access.cors.allowed-origins", " https://a.example.com , ,https://b.example.com ,, "));

		assertThat(properties.cors().allowedOrigins()).containsExactly("https://a.example.com",
				"https://b.example.com");
	}

	@Test
	void anEmptyOriginsValueMeansNoOrigin() {
		ApiAccessProperties properties = bind(Map.of("app.access.api-key", "", "app.access.cors.allowed-origins", ""));

		assertThat(properties.cors().allowedOrigins()).isEmpty();
		assertThat(properties.apiKey()).isEmpty();
		assertThat(properties.apiKeyEnabled()).isFalse();
	}

	@Test
	void absentPropertiesGiveSafeDefaults() {
		ApiAccessProperties properties = bind(Map.of());

		assertThat(properties.apiKey()).isEmpty();
		assertThat(properties.apiKeyEnabled()).isFalse();
		assertThat(properties.cors().allowedOrigins()).isEmpty();
	}

	@Test
	void aBlankApiKeyIsNotEnabled() {
		assertThat(new ApiAccessProperties("   ", null).apiKeyEnabled()).isFalse();
		assertThat(new ApiAccessProperties("x", null).apiKeyEnabled()).isTrue();
	}

	@Test
	void toStringNeverContainsTheKey() {
		ApiAccessProperties properties = new ApiAccessProperties("super-segredo-123", null);

		assertThat(properties.toString()).doesNotContain("super-segredo-123").contains("apiKeyConfigured=true");
	}

	@Test
	void productionRefusesToStartWithoutAKey() {
		assertThatThrownBy(() -> new ApiKeyRequired(new ApiAccessProperties("", null)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_API_KEY");
		assertThatThrownBy(() -> new ApiKeyRequired(new ApiAccessProperties("   ", null)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_API_KEY");
		assertThatThrownBy(() -> new ApiKeyRequired(new ApiAccessProperties(null, null)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_API_KEY");
	}

	@Test
	void productionStartsWithAKeyAndTheMessageHasNoValues() {
		new ApiKeyRequired(new ApiAccessProperties("chave", null));

		assertThatThrownBy(() -> new ApiKeyRequired(new ApiAccessProperties("", null)))
				.hasMessageNotContaining("chave");
	}

}
