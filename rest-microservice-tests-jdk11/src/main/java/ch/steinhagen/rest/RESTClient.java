package ch.steinhagen.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

import ch.steinhagen.rest.RESTServer.MimeType;
import ch.steinhagen.rest.tcp.TCPServer;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.spi.utils.Tuple;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * Test in response to CSCO-AP's REST performance evaluation
 * 
 * based on list of REST frameworkds and benchmark for small responses (ie. "Hello World!") found at:
 * https://www.techempower.com/benchmarks/#section=test&runid=a0d523de-091b-4008-b15d-bd4c8aa25066&hw=ph&test=plaintext&l=xan9tr-3&a=2
 * 
 * @author rstein
 */
public class RESTClient { // NOPMD - nomen est omen
	private static final Logger LOGGER = LoggerFactory.getLogger(RESTClient.class);
	private static final String HTTP_LOCALHOST = "http://localhost:";
	private static final String REST_BYTE_BUFFER = "/byteBuffer";
	private static final String SERVER_SPARK = HTTP_LOCALHOST + RESTServer.SERVER_SPARK;
	private static final String SERVER_JAVALIN = HTTP_LOCALHOST + RESTServer.SERVER_JAVALIN;
	private static final String SERVER_RAPIDOID = HTTP_LOCALHOST + RESTServer.SERVER_RAPIDOID;
	private static final String SERVER_MOCK_HTTP = HTTP_LOCALHOST + RESTServer.SERVER_MOCK_HTTP;
	private static final String SERVER_WEBFLUX = HTTP_LOCALHOST + RESTServer.SERVER_WEBFLUX;
	private static final String SERVER_ACT = HTTP_LOCALHOST + RESTServer.SERVER_ACT;
	private static final String SERVER_TCP = HTTP_LOCALHOST + RESTServer.SERVER_TCP;
	private static Gson gson = RESTServer.initGson();

	private static final HttpClient CLIENT_JDK = HttpClient.newHttpClient();
	private static OkHttpClient okClient = new OkHttpClient();
	private static byte[] bBuffer = new byte[4000000];
	private static EventSource sseSource;
	private static WebFluxClient webFlux = new WebFluxClient();
	private static FastByteBuffer fastByteBuffer = new FastByteBuffer((int) (FastByteBuffer.SIZE_OF_DOUBLE * 1000000));

	private static final EventSourceListener EVENT_SOURCE_LISTENER = new EventSourceListener() {
		private final BlockingQueue<Object> events = new LinkedBlockingDeque<>();

		@Override
		public void onEvent(final EventSource eventSource, final String id, final String type, String data) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.atInfo().log("[ES] onEvent");
			}
			events.add(new Event(id, type, data));
		}
	};

	private static EventSource.Factory factory = EventSources.createFactory(okClient);

	private RESTClient() {
		// empty constructor - utility class
	}
	
	public static String get(final String path) {
		return get(path, MimeType.PLAINTEXT);
	}

	public static String get(final String path, MimeType mime) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(path)).GET().header("accept", mime.toString())
				.build();

		try {
			HttpResponse<String> response = CLIENT_JDK.send(request, BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException e) {
			LOGGER.atError().setCause(e);
		}

		return null;
	}

	public static byte[] getByteArrayJDK(final String path, MimeType mimeType) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(path)).GET().version(HttpClient.Version.HTTP_2)
				.header("Accept", mimeType.toString()).build();

		try {
			switch (mimeType) {
			case PLAINTEXT:
			case HTML:
			case JSON:
			case XML:
				HttpResponse<String> stringResponse = CLIENT_JDK.send(request, BodyHandlers.ofString());
				MyBinaryData json = gson.fromJson(stringResponse.body(), MyBinaryData.class);
				return json.binaryData;
			case UNKNOWN:
			case BINARY:
			default:
				HttpResponse<byte[]> byteResponse = CLIENT_JDK.send(request, BodyHandlers.ofByteArray());
				return byteResponse.body();
			}

		} catch (IOException | InterruptedException e) {
			LOGGER.atError().setCause(e);
		}

		return new byte[0];
	}

	public static byte[] getByteArrayOkHTTP(final String path, final MimeType mimeType, boolean useGSON) {
		Request request = new Request.Builder().url(path).get().addHeader("Accept", mimeType.toString()).build();

		try (Response response = okClient.newCall(request).execute()) {
			switch (mimeType) {
			case PLAINTEXT:
			case HTML:
			case JSON:
			case XML:
				MyBinaryData json;
				if (useGSON) {
					Reader reader = response.body().charStream();
					json = gson.fromJson(reader, MyBinaryData.class);
				} else {
					json = JSON.parseObject(response.body().bytes(), MyBinaryData.class);
				}
				return json.binaryData;
			case UNKNOWN:
			case BINARY:
			default:
				return response.body().bytes();
			}
		} catch (IOException e) {
			LOGGER.atError().setCause(e);
		}

		return new byte[0];
	}

	public static Tuple<byte[], Integer> getByteArrayTcp(final String path, final MimeType mimeType) {
		try (Socket clientSocket = new Socket("localhost", RESTServer.SERVER_TCP);
				final OutputStream out = clientSocket.getOutputStream();
				final InputStream in = clientSocket.getInputStream();) {
			byte[] datagramSize = new byte[4];

			// DataOutputStream outToServer = new DataOutputStream(out);
			// BufferedInputStream inFromServer = new BufferedInputStream(in);
			// write trigger request
			if (mimeType.equals(MimeType.JSON)) {
				out.write("GET_JSON\0".getBytes());
			} else {
				out.write("GET_BYTE\0".getBytes());
			}
			in.read(datagramSize, 0, 4);
			int nBytesToRead = TCPServer.fromByteArray(datagramSize);
			int nBytesRead = in.readNBytes(bBuffer, 0, nBytesToRead);
			while (nBytesRead != nBytesToRead) {
				nBytesRead += in.readNBytes(bBuffer, nBytesRead, bBuffer.length - nBytesRead);
				System.err.println("needed to re-read additional bytes during iteration");
			}
			if (mimeType.equals(MimeType.JSON)) {
				String data = new String(bBuffer, 0, nBytesRead);
				MyBinaryData json = gson.fromJson(data, MyBinaryData.class);
				return new Tuple<>(json.binaryData, json.binaryData.length);
			}
			// System.err.println("read bytes = " + nBytesRead + " size of get = " +
			// "GET\0".getBytes().length);
			return new Tuple<>(bBuffer, nBytesRead);
		} catch (IOException e) {
			LOGGER.atError().setCause(e);
		}
		return null;
	}

	public static FastByteBuffer getFastBufferSynchronous(final String path) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(path)).GET().build();

		try {
			HttpResponse<byte[]> response = CLIENT_JDK.send(request, BodyHandlers.ofByteArray());
			System.err.println("response size = " + response.body().length);
			fastByteBuffer.reset();
			fastByteBuffer.putByteArray(response.body());
			fastByteBuffer.reset();
			return fastByteBuffer;
		} catch (IOException | InterruptedException e) {
			LOGGER.atError().setCause(e);
		}

		return fastByteBuffer;
	}

	public static void main(String[] args) {
		LOGGER.atInfo().log("get 1 = " + get(SERVER_SPARK + "/hello"));
		LOGGER.atInfo().log("get 1a = " + get(SERVER_SPARK + "/hello", MimeType.JSON));
		LOGGER.atInfo().log("get 1b = " + get(SERVER_SPARK + "/hello", MimeType.HTML));
		LOGGER.atInfo().log("get 1c = " + get(SERVER_SPARK + "/hello", MimeType.BINARY));
		//LOGGER.atInfo().log("get ACT-JSON = " + get(SERVER_ACT + "/byteBuffer", MimeType.JSON).substring(0, 60) + "\n");
//		LOGGER.atInfo().log("get ACT-HTML = " + get(host5 + "/byteBuffer", MimeType.HTML).substring(0, 60) + "\n");
        //		LOGGER.atInfo().log(
        //				"get ACT-BINARY = " + get(SERVER_ACT + "/byteBuffer", MimeType.BINARY).substring(0, 60) + "\n");

		LOGGER.atInfo().log("\n");
		LOGGER.atInfo().log("get 2 = " + get(SERVER_SPARK + "/hello/Tom"));
		LOGGER.atInfo().log("get 3 = " + get(SERVER_JAVALIN + "/"));
		// LOGGER.atInfo().log("get 3 = " + get(host+"/byteBuffer"));
		LOGGER.atInfo().log("\n");
		sseSource = newEventSource(SERVER_JAVALIN + "/sse");

		final String[][] serverID = { { "spark", SERVER_SPARK }, { "javalin", SERVER_JAVALIN },
				{ "rapidoid", SERVER_RAPIDOID }, { "WebFlux", SERVER_WEBFLUX }, { "ACT", SERVER_ACT } };
		final int nRuns = 1;
		final int nIterations = 1000;
		for (int r = 0; r < nRuns; r++) {
			LOGGER.atInfo().log("start run #" + r);
			MimeType[] protocols = { MimeType.JSON, MimeType.BINARY };
			for (MimeType protocol : protocols) {
				for (int i = 0; i < serverID.length; i++) {
					for (HttpClientInterface httpClient : HttpClientInterface.values()) {
						// skip TCP
						if (httpClient.equals(HttpClientInterface.TCP_REFERENCE)) {
							continue;
						}
						testByteArrayRetrieval(serverID[i][0], serverID[i][1], httpClient, protocol, nIterations);
					}
				}
			}
			// reference case/implementation bare minimum TCP comm
			testByteArrayRetrieval("TCP ref", "", HttpClientInterface.TCP_REFERENCE, MimeType.JSON, nIterations);
			testByteArrayRetrieval("TCP ref", "", HttpClientInterface.TCP_REFERENCE, MimeType.BINARY, nIterations);
			LOGGER.atInfo().log("run #" + r + " - done");
		}
	}

	private static EventSource newEventSource(String path) {
		Request request = new Request.Builder().url(path).build();
		return factory.newEventSource(request, EVENT_SOURCE_LISTENER);
	}

	public static double testByteArrayRetrieval(final String caseString, final String host,
			final HttpClientInterface httpClient, final MimeType mimeType, final int nIterations) {
		long start = System.currentTimeMillis();
		long nBytes = 0;
		long hash = 0;

		try {
			for (int i = 0; i < nIterations; i++) {
				byte[] data = null;
				int nBytesReceived = 0;
				Tuple<byte[], Integer> dataTupple = null;
				switch (httpClient) {
				case TCP_REFERENCE:
					dataTupple = getByteArrayTcp(host + REST_BYTE_BUFFER, mimeType);
					if (dataTupple != null) {
						data = dataTupple.getXValue();
						nBytesReceived = dataTupple.getYValue().intValue();
					}
					break;
				case JDK_HTTP_CLIENT:
					data = getByteArrayJDK(host + REST_BYTE_BUFFER, mimeType);
					if (data != null) {
						nBytesReceived = data.length;
					}
					break;
				case WEBFLUX_HTTP_CLIENT:
					data = null;
					switch (mimeType) {
					case PLAINTEXT:
					case HTML:
					case JSON:
					case XML:
						data = webFlux.getByteBufferJSON(host + REST_BYTE_BUFFER);
						break;
					case UNKNOWN:
					case BINARY:
					default:
						data = webFlux.getByteBufferBinary(host + REST_BYTE_BUFFER);
						break;
					}

					if (data != null) {
						nBytesReceived = data.length;
					}
					break;
				case OK_HTTP_CLIENT_FASTJSON:
					data = getByteArrayOkHTTP(host + REST_BYTE_BUFFER, mimeType, false);
					if (data != null) {
						nBytesReceived = data.length;
					}
					break;
				case OK_HTTP_CLIENT_GSON:
				default:
					data = getByteArrayOkHTTP(host + REST_BYTE_BUFFER, mimeType, true);
					if (data != null) {
						nBytesReceived = data.length;
					}
					break;
				}

				if (data == null || nBytesReceived == 0) {
					LOGGER.atWarn().log("null or zero data received");
				} else {
					nBytes += nBytesReceived;
					hash += (31 + data[0]) % 41;
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				final String msg = String.format("error for %-10s-%5s via %25s - %s", caseString, mimeType.name(), httpClient,
						e.getLocalizedMessage());
				LOGGER.atError().setCause(e).log(msg);
			}
			return 0.0;
		}

		long stop = System.currentTimeMillis();
		double diff = 1e-3 * (stop - start);
		double throughput = nBytes / Math.pow(2, 20) / diff;
		final String msg = String.format("Result: %-9s-%7s via %25s: streamed %d bytes within %5.2f seconds -> %7.1f MB/s - hash = %d",
				caseString, mimeType.name(), httpClient, nBytes, diff, throughput, hash);
		LOGGER.atInfo().log(msg);
		return throughput;
	}

	private enum HttpClientInterface {
		JDK_HTTP_CLIENT, OK_HTTP_CLIENT_GSON, OK_HTTP_CLIENT_FASTJSON, WEBFLUX_HTTP_CLIENT, TCP_REFERENCE;
	}
}
