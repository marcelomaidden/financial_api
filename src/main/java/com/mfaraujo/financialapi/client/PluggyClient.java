package com.mfaraujo.financialapi.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mfaraujo.financialapi.client.dto.PluggyAccount;
import com.mfaraujo.financialapi.client.dto.PluggyBill;
import com.mfaraujo.financialapi.client.dto.PluggyCursorPage;
import com.mfaraujo.financialapi.client.dto.PluggyErrorResponse;
import com.mfaraujo.financialapi.client.dto.PluggyPage;
import com.mfaraujo.financialapi.client.dto.PluggyTransaction;
import com.mfaraujo.financialapi.config.PluggyProperties;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.exception.UnknownOwnerException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Ponto de entrada para a Pluggy. Mantém um {@link PluggyApi} por dono, cada um
 * com as credenciais desse dono, e recebe o dono em cada chamada.
 */
@Component
public class PluggyClient {

	private final Map<String, PluggyApi> apisByOwner;

	private final JsonMapper jsonMapper;

	public PluggyClient(PluggyProperties properties, RestClient.Builder restClientBuilder,
			PluggyHttpRequestFactory requestFactory, PluggyApiKeyProvider apiKeyProvider, JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
		this.apisByOwner = properties.owners().stream()
				.map(PluggyProperties.Owner::name)
				.collect(Collectors.toUnmodifiableMap(Function.identity(),
						owner -> createApi(owner, properties, restClientBuilder, requestFactory, apiKeyProvider)));
	}

	public PluggyPage<PluggyAccount> listAccounts(String owner, String itemId) {
		return api(owner).listAccounts(itemId);
	}

	public PluggyCursorPage<PluggyTransaction> listTransactions(String owner, String accountId, LocalDate dateFrom,
			LocalDate dateTo, String after) {
		return api(owner).listTransactions(accountId, formatDate(dateFrom), formatDate(dateTo), after);
	}

	private static String formatDate(LocalDate date) {
		return date == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format(date);
	}

	public PluggyPage<PluggyBill> listBills(String owner, String accountId, Integer page, Integer pageSize) {
		return api(owner).listBills(accountId, page, pageSize);
	}

	private PluggyApi api(String owner) {
		PluggyApi api = apisByOwner.get(owner);
		if (api == null) {
			throw new UnknownOwnerException(owner);
		}
		return api;
	}

	private PluggyApi createApi(String owner, PluggyProperties properties, RestClient.Builder restClientBuilder,
			PluggyHttpRequestFactory requestFactory, PluggyApiKeyProvider apiKeyProvider) {
		RestClient restClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory.get())
				.requestInterceptor(new PluggyApiKeyInterceptor(owner, apiKeyProvider))
				.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
					throw new PluggyException(owner, response.getStatusCode(), errorMessage(response));
				})
				.build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.build()
				.createClient(PluggyApi.class);
	}

	private String errorMessage(ClientHttpResponse response) {
		try {
			PluggyErrorResponse error = jsonMapper.readValue(response.getBody(), PluggyErrorResponse.class);
			return error != null ? error.bestMessage() : null;
		}
		catch (Exception ex) {
			return null;
		}
	}

}
