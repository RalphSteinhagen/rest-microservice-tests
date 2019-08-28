package ch.steinhagen.rest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.gson.Gson;

import reactor.core.publisher.Mono;

public class WebFluxClient {
	private static Gson gson = RESTServer.initGson();
	private Map<String, WebClient> clientCache = new ConcurrentHashMap<>();
	private WebClient client; // N.B. clients are immutable, i.e. one client per URL

	WebFluxClient() {
		final String url = "http://localhost:8084";
		client = WebClient.create(url);
		clientCache.put(url, client);
	}

	public String getTestResult() {
		Mono<ClientResponse> result = client.get().uri("/hello").accept(org.springframework.http.MediaType.TEXT_PLAIN)
				.exchange();
		return ">> result = " + result.flatMap(res -> res.bodyToMono(String.class)).block();
	}

	public String get(final String urlEndPoint) {
		client = clientCache.computeIfAbsent(urlEndPoint, WebClient::create);
		Mono<ClientResponse> result = client.get().uri(urlEndPoint)
				.accept(org.springframework.http.MediaType.TEXT_PLAIN).exchange();
		return ">> result = " + result.flatMap(res -> res.bodyToMono(String.class)).block();
	}

	public byte[] getByteBufferJSON(final String urlEndPoint) {
		client = clientCache.computeIfAbsent(urlEndPoint, WebClient::create);

		Mono<ClientResponse> result = client.get().uri(urlEndPoint)
				.accept(org.springframework.http.MediaType.APPLICATION_JSON).exchange();
		String strResult = result.flatMap(res -> res.bodyToMono(String.class)).block();
		MyBinaryData json = gson.fromJson(strResult, MyBinaryData.class);
		return json.binaryData;

	}

	public byte[] getByteBufferBinary(final String urlEndPoint) {
		client = clientCache.computeIfAbsent(urlEndPoint, WebClient::create);
		Mono<byte[]> result = client.get().uri(urlEndPoint)
				.accept(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM).exchange()
	            .flatMap(response -> response.bodyToMono(ByteArrayResource.class))
	            .map(ByteArrayResource::getByteArray);
		
		final byte[] byteResult = result.block();
		return byteResult;
	}
}
