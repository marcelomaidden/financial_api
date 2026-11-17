package com.mfaraujo.financialapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfaraujo.financialapi.config.ApiAccessProperties;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyFilterTest {

	private static final String KEY = "chave-secreta-de-teste";

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private final ApiKeyFilter filter = new ApiKeyFilter(new ApiAccessProperties(KEY, null), jsonMapper);

	private Result call(ApiKeyFilter target, String method, String header) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(method, "/accounts");
		if (header != null) {
			request.addHeader(ApiKeyFilter.HEADER, header);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		target.doFilter(request, response, chain);
		return new Result(response, chain.getRequest() != null);
	}

	private record Result(MockHttpServletResponse response, boolean chainCalled) {
	}

	@Test
	void theRightKeyPasses() throws Exception {
		Result result = call(filter, "GET", KEY);

		assertThat(result.chainCalled()).isTrue();
		assertThat(result.response().getStatus()).isEqualTo(200);
	}

	@Test
	void aMissingKeyIsUnauthorizedAndStopsTheChain() throws Exception {
		Result result = call(filter, "GET", null);

		assertThat(result.chainCalled()).isFalse();
		assertThat(result.response().getStatus()).isEqualTo(401);
	}

	@Test
	void aWrongKeyIsUnauthorizedAndStopsTheChain() throws Exception {
		Result result = call(filter, "GET", "outra-chave");

		assertThat(result.chainCalled()).isFalse();
		assertThat(result.response().getStatus()).isEqualTo(401);
	}

	@Test
	void aBlankKeyHeaderIsUnauthorized() throws Exception {
		assertThat(call(filter, "GET", "").response().getStatus()).isEqualTo(401);
		assertThat(call(filter, "GET", "   ").response().getStatus()).isEqualTo(401);
	}

	@Test
	void aKeyWithADifferentCaseIsUnauthorized() throws Exception {
		assertThat(call(filter, "GET", KEY.toUpperCase()).response().getStatus()).isEqualTo(401);
	}

	@Test
	void theBodyFollowsTheStandardErrorFormatAsJson() throws Exception {
		MockHttpServletResponse response = call(filter, "GET", "errada").response();

		assertThat(response.getContentType()).startsWith("application/json");
		JsonNode body = jsonMapper.readTree(response.getContentAsString());
		assertThat(body.path("status").asInt()).isEqualTo(401);
		assertThat(body.path("error").asString()).isEqualTo("Unauthorized");
		assertThat(body.path("message").asString()).isNotBlank();
	}

	@Test
	void missingAndWrongKeysGetTheSameMessage() throws Exception {
		String missing = call(filter, "GET", null).response().getContentAsString();
		String wrong = call(filter, "GET", "errada").response().getContentAsString();

		assertThat(missing).isEqualTo(wrong);
	}

	@Test
	void theResponseNeverContainsTheSentOrTheConfiguredKey() throws Exception {
		String body = call(filter, "GET", "chave-enviada-123").response().getContentAsString();

		assertThat(body).doesNotContain("chave-enviada-123").doesNotContain(KEY);
	}

	@Test
	void optionsPassesWithoutAKey() throws Exception {
		Result result = call(filter, "OPTIONS", null);

		assertThat(result.chainCalled()).isTrue();
		assertThat(result.response().getStatus()).isEqualTo(200);
	}

	@Test
	void aBlankConfiguredKeyTurnsTheCheckOff() throws Exception {
		ApiKeyFilter off = new ApiKeyFilter(new ApiAccessProperties("", null), jsonMapper);

		assertThat(call(off, "GET", null).chainCalled()).isTrue();
		assertThat(call(off, "GET", "qualquer").chainCalled()).isTrue();
	}

	@Test
	void everyHttpMethodRequiresTheKeyExceptOptions() throws Exception {
		for (String method : new String[] { "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD" }) {
			assertThat(call(filter, method, null).response().getStatus()).as(method).isEqualTo(401);
			assertThat(call(filter, method, KEY).chainCalled()).as(method).isTrue();
		}
	}

}
