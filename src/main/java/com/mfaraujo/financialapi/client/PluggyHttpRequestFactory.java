package com.mfaraujo.financialapi.client;

import java.net.http.HttpClient;
import java.util.function.Supplier;

import com.mfaraujo.financialapi.config.PluggyProperties;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;

/** Fábrica de requisições HTTP para a Pluggy, com os timeouts configurados. */
@Component
public class PluggyHttpRequestFactory implements Supplier<ClientHttpRequestFactory> {

	private final ClientHttpRequestFactory requestFactory;

	public PluggyHttpRequestFactory(PluggyProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(properties.readTimeout());
		this.requestFactory = factory;
	}

	@Override
	public ClientHttpRequestFactory get() {
		return requestFactory;
	}

}
