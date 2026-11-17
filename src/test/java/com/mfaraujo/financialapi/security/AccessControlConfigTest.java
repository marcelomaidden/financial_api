package com.mfaraujo.financialapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.mfaraujo.financialapi.config.ApiAccessProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Os dois filtros juntos, na ordem em que o projeto os registra (CORS e depois a chave), diante
 * de um controller de teste. Prova a interação entre eles sem subir a aplicação.
 */
class AccessControlConfigTest {

	private static final String KEY = "chave-de-teste";

	private static final String FRONT = "https://front.exemplo.com";

	private static final String OTHER = "https://outro.exemplo.com";

	@RestController
	static class Dummy {

		@GetMapping("/accounts")
		String accounts() {
			return "[]";
		}

	}

	private static MockMvc mvc(ApiAccessProperties properties) {
		return MockMvcBuilders.standaloneSetup(new Dummy())
				.addFilters(AccessControlConfig.corsFilter(properties),
						new ApiKeyFilter(properties, JsonMapper.builder().build()))
				.build();
	}

	private final MockMvc mvc = mvc(new ApiAccessProperties(KEY, new ApiAccessProperties.Cors(List.of(FRONT))));

	@Test
	void anAllowedOriginWithTheKeyGetsTheDataAndTheCorsHeader() throws Exception {
		mvc.perform(get("/accounts").header("Origin", FRONT).header("X-API-Key", KEY)).andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT));
	}

	@Test
	void the401OfAnAllowedOriginCarriesTheCorsHeaderSoTheBrowserCanReadIt() throws Exception {
		mvc.perform(get("/accounts").header("Origin", FRONT)).andExpect(status().isUnauthorized())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void aWrongKeyFromAnAllowedOriginAlsoCarriesTheCorsHeader() throws Exception {
		mvc.perform(get("/accounts").header("Origin", FRONT).header("X-API-Key", "errada"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT));
	}

	@Test
	void anUnlistedOriginIsRejectedWithoutCorsHeaders() throws Exception {
		mvc.perform(get("/accounts").header("Origin", OTHER).header("X-API-Key", KEY))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void thePreflightOfAnAllowedOriginAsksForTheKeyHeaderAndPassesWithoutIt() throws Exception {
		mvc.perform(options("/accounts").header("Origin", FRONT).header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "x-api-key")).andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT))
				.andExpect(header().string("Access-Control-Allow-Methods", "GET,OPTIONS"))
				.andExpect(header().string("Access-Control-Allow-Headers", "x-api-key"))
				.andExpect(header().string("Access-Control-Max-Age", "3600"));
	}

	@Test
	void thePreflightAcceptsContentTypeAndAcceptToo() throws Exception {
		mvc.perform(options("/accounts").header("Origin", FRONT).header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "x-api-key,content-type,accept"))
				.andExpect(status().isOk());
	}

	@Test
	void thePreflightOfAnUnlistedOriginIsRejected() throws Exception {
		mvc.perform(options("/accounts").header("Origin", OTHER).header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "x-api-key")).andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void thePreflightRejectsAMethodThatIsNotAllowed() throws Exception {
		mvc.perform(options("/accounts").header("Origin", FRONT).header("Access-Control-Request-Method", "DELETE")
				.header("Access-Control-Request-Headers", "x-api-key")).andExpect(status().isForbidden());
	}

	@Test
	void credentialsAreNotAllowed() throws Exception {
		mvc.perform(get("/accounts").header("Origin", FRONT).header("X-API-Key", KEY))
				.andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
	}

	@Test
	void anUnknownRouteWithoutTheKeyIsUnauthorizedAndNotNotFound() throws Exception {
		mvc.perform(get("/nao-existe")).andExpect(status().isUnauthorized());
	}

	@Test
	void aRequestWithoutOriginLikePostmanNeedsOnlyTheKey() throws Exception {
		mvc.perform(get("/accounts").header("X-API-Key", KEY)).andExpect(status().isOk())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
		mvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
	}

	@Test
	void anEmptyOriginListAllowsNoBrowserOrigin() throws Exception {
		MockMvc none = mvc(new ApiAccessProperties(KEY, new ApiAccessProperties.Cors(List.of())));

		none.perform(get("/accounts").header("Origin", FRONT).header("X-API-Key", KEY))
				.andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
		none.perform(get("/accounts").header("X-API-Key", KEY)).andExpect(status().isOk());
	}

	@Test
	void severalOriginsCanBeListed() throws Exception {
		MockMvc two = mvc(new ApiAccessProperties(KEY, new ApiAccessProperties.Cors(List.of(FRONT, OTHER))));

		two.perform(get("/accounts").header("Origin", OTHER).header("X-API-Key", KEY))
				.andExpect(header().string("Access-Control-Allow-Origin", OTHER));
		two.perform(get("/accounts").header("Origin", FRONT).header("X-API-Key", KEY))
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT));
	}

	@Test
	void aTrailingSlashInTheConfiguredOriginIsTolerated() throws Exception {
		MockMvc slash = mvc(new ApiAccessProperties(KEY, new ApiAccessProperties.Cors(List.of(FRONT + "/"))));

		slash.perform(get("/accounts").header("Origin", FRONT).header("X-API-Key", KEY)).andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT));
	}

	@Test
	void anOriginWithADifferentSchemeOrPortDoesNotMatch() throws Exception {
		mvc.perform(get("/accounts").header("Origin", "http://front.exemplo.com").header("X-API-Key", KEY))
				.andExpect(status().isForbidden());
		mvc.perform(get("/accounts").header("Origin", "https://front.exemplo.com:8443").header("X-API-Key", KEY))
				.andExpect(status().isForbidden());
	}

	@Test
	void theCorsFilterIsRegisteredBeforeTheApiKeyFilter() {
		ApiAccessProperties properties = new ApiAccessProperties(KEY, null);
		AccessControlConfig config = new AccessControlConfig();

		int cors = config.corsFilterRegistration(properties).getOrder();
		int apiKey = config.apiKeyFilterRegistration(properties, JsonMapper.builder().build()).getOrder();

		assertThat(cors).isLessThan(apiKey);
	}

}
