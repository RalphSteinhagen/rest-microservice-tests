package ch.steinhagen.rest;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static spark.Spark.get;
import static spark.Spark.port;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.rapidoid.config.Conf;
import org.rapidoid.http.MediaType;
import org.rapidoid.setup.Admin;
import org.rapidoid.setup.App;
import org.rapidoid.setup.AppBootstrap;
import org.rapidoid.setup.On;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import ch.steinhagen.rest.tcp.TCPServer;
import de.gsi.dataset.utils.serializer.BinarySerialiser;
import de.gsi.dataset.utils.serializer.FastByteBuffer;
import io.javalin.Javalin;
import io.javalin.core.compression.CompressionStrategy;
import io.javalin.core.compression.Gzip;
import io.javalin.http.sse.SseClient;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spark.Request;

//import act.Act;
//import org.osgl.mvc.annotation.GetAction;

/**
 * Test in response to CSCO-AP's REST performance evaluation
 * 
 * based on list of REST frameworkds and benchmark for small responses (ie.
 * "Hello World!") found at:
 * https://www.techempower.com/benchmarks/#section=test&runid=a0d523de-091b-4008-b15d-bd4c8aa25066&hw=ph&test=plaintext&l=xan9tr-3&a=2
 * 
 * 
 * @author rstein
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = EmbeddedWebServerFactoryCustomizerAutoConfiguration.class)
public class RESTServer { // NOPMD - nomen est omen
	private static final Logger LOGGER = LoggerFactory.getLogger(RESTServer.class);
	private static final int N_DATA = 1000000;
	private static final String ENDPOINT_HELLO = "/hello";
	private static final String ENDPOINT_BYTE_BUFFER = "/byteBuffer";
	private static final RESTServer SELF = new RESTServer();
	private static final int SERVER_PORT0 = 8080;
	public static final int SERVER_SPARK = SERVER_PORT0 + 0;
	public static final int SERVER_JAVALIN = SERVER_PORT0 + 1;
	public static final int SERVER_RAPIDOID = SERVER_PORT0 + 2;
	public static final int SERVER_MOCK_HTTP = SERVER_PORT0 + 3;
	public static final int SERVER_WEBFLUX = SERVER_PORT0 + 4;
	public static final int SERVER_ACT = SERVER_PORT0 + 5;
	public static final int SERVER_TCP = SERVER_PORT0 + 10;

	private static final byte[] BYTE_BUFFER = new byte[N_DATA];
	private static Gson gson = initGson();
	private static ObjectMapper jackson = new ObjectMapper();

	private static Thread sseThread;
	private static final ConcurrentLinkedQueue<SseClient> SSE_CLIENTS = new ConcurrentLinkedQueue<>();
	private static final DefaultDataBufferFactory FACTORY = new DefaultDataBufferFactory();
	private static final MockWebServer HANDLER_MOCK_OK_HTTP_SERVER = new MockWebServer();
	private static final TCPServer HANDLER_TCP_SERVER = new TCPServer("localhost", SERVER_TCP);

	public RESTServer() {
		// empty constructor - de-facto utility class
	}

	public static void initData() {
		for (int i = 0; i < BYTE_BUFFER.length; i++) {
			BYTE_BUFFER[i] = (byte) (65 + i % 128);
		}		
	}

	public static Gson initGson() {
		// Gson type adapter to serialise and deserialise byte arrays in base64
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc,
				context) -> new JsonPrimitive(Base64.getEncoder().encodeToString(src)));
		builder.registerTypeAdapter(byte[].class,
				(JsonDeserializer<byte[]>) (json, typeOfT, context) -> Base64.getDecoder().decode(json.getAsString()));
		return builder.create();
	}

	public static void main(String[] args) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("java version = " + System.getProperty("java.version"));
			LOGGER.atInfo().log("log config = " + System.getProperty("log4j.configurationFile"));
		}
		initData();

		startActServer(args);

		startSparkServer();

		startJavalinServer();

		startRapidoidServer(args);

		startSpringWebFluxServer(args);

		startMockOkHttpServer();

		startTcpServer();

		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init of all server finished");
		}
	}

	// @GetAction
//	public String sayHello() {
//		return "Hello World!";
//	}
//
//	@GetAction("/bye")
//	public String sayBye() {
//		return "Bye!";
//	}
//
	// https://github.com/actframework/actframework
	// http://actframework.org/doc/get_start.md
	// http://actframework.org/doc/reference/configuration.md
	public static void startActServer(final String[] args) {
		System.setProperty("http.port", Integer.toString(8085));
		System.setProperty("http.port", Integer.toString(8085));
//		try {
//			Act.start();
//		} catch (Exception e) {
//			LOGGER.atError().setCause(e);
//		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init actServer(...)");
		}
	}

	public static void startJavalinServer() {
		Javalin app = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
			// config.defaultContentType = MimeType.BINARY.toString();
			config.compressionStrategy(null, new Gzip(0));
			config.inner.compressionStrategy = CompressionStrategy.NONE;
		});
		app.start(SERVER_JAVALIN);
		app.get("/", ctx -> ctx.result("root directory"));
		app.get(ENDPOINT_HELLO, ctx -> ctx.result("Javalin: Hello World"));
		app.get("/hello/:name", ctx -> {
			ctx.result("Javalin: Hello: " + ctx.pathParam("name") + "!");
		});

		app.get(ENDPOINT_BYTE_BUFFER, ctx -> {
			String type = ctx.header("Accept");
			if (type == null || type.equalsIgnoreCase(MimeType.JSON.toString())) {
				// ctx.contentType(MimeType.JSON.toString()).result(gson.toJson(new
				// MyBinaryData(buffer)));
				ctx.contentType(MimeType.JSON.toString()).result(JSON.toJSONString(new MyBinaryData(BYTE_BUFFER)));
//				ctx.contentType(MimeType.JSON.toString()).result(JavalinJson.toJson(new MyBinaryData(buffer)));

				return;
			} else if (type.equalsIgnoreCase(MimeType.BINARY.toString())) {
				try (InputStream targetStream = new ByteArrayInputStream(BYTE_BUFFER)) {
					// normal mime type
					ctx.contentType(MimeType.BINARY.toString()).result(targetStream);
				} catch (Exception e) {
					LOGGER.atError().setCause(e);
				}
				return;
			}

			try (InputStream targetStream = new ByteArrayInputStream(BYTE_BUFFER)) {
				// normal mime type
				ctx.contentType(MimeType.BINARY.toString()).result(targetStream);
			} catch (Exception e) {
				LOGGER.atError().setCause(e);
			}
		});

		// https://javalin.io/news/2019/01/17/javalin-2.6.0-released.html
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("before sse");
		}
		app.sse("/sse", client -> {
			SSE_CLIENTS.add(client);
			client.sendEvent("connected", "Hello, SSE " + SSE_CLIENTS.toString());
			client.onClose(SSE_CLIENTS::remove);
		});
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("before thread");
		}
		sseThread = new Thread(() -> {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.atInfo().log("started thread");
			}
			while (true) {
				if (!SSE_CLIENTS.isEmpty()) {
					if (LOGGER.isInfoEnabled()) {
						LOGGER.atInfo().log("JavalinServer - sending PING to clients: " + SSE_CLIENTS.toString());
					}
				}
				for (SseClient client : SSE_CLIENTS) {

					client.sendEvent("PING " + System.currentTimeMillis() / 1000);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.atError().setCause(e);
				}
			}
		});
		sseThread.start();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init javalinServer()");
		}
	}

	public static void startMockOkHttpServer() {
		try {
			HANDLER_MOCK_OK_HTTP_SERVER.start(SERVER_MOCK_HTTP);

			// Ask the server for its URL. You'll need this to make HTTP requests.
			HttpUrl baseUrl = HANDLER_MOCK_OK_HTTP_SERVER.url("/");
		} catch (IOException e) {
			LOGGER.atError().setCause(e);
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init mockOkHttpServer()");
		}
	}

	public static void startRapidoidServer(final String[] args) {
		// first thing to do - initializing Rapidoid, without bootstrapping anything at
		// the moment
		AppBootstrap bootstrap = App.run(args); // instead of App.bootstrap(args), which might start the server
		// bootstrap.metrics();

		// customising the server address and port - before the server is bootstrapped
		On.address("0.0.0.0").port(SERVER_RAPIDOID);
		Admin.address("127.0.0.1").port(9999);

		// fine-tuning the HTTP server
		// Conf.HTTP.set("maxPipeline", 32);
		// Conf.NET.set("bufSizeKB", 16);

		Conf.HTTP.set("maxPipeline", 32);
		Conf.NET.set("bufSizeKB", 2048);

		On.get("/size").json((String msg) -> msg.length());
		On.get(ENDPOINT_HELLO).json((String msg) -> msg = "Rapidoid: Hello World!");
		On.get("/test").html((req, resp) -> "this is a test!");
		On.get(ENDPOINT_BYTE_BUFFER).contentType(MediaType.BINARY).managed(false).html(BYTE_BUFFER);
//		ON.GET(ENDPOINT_BYTE_BUFFER).CONTENTTYPE(MEDIATYPE.APPLICATION_JSON)
//				.HTML(JSON.TOJSONSTRING(NEW MYBINARYDATA(BUFFER)));
//		On.get(ENDPOINT_BYTE_BUFFER).contentType(MediaType.APPLICATION_JSON).html((req, resp) -> {
//					return JSON.toJSONString(new MyBinaryData(buffer));	
//				});

		On.get(ENDPOINT_BYTE_BUFFER).managed(false).html((req, resp) -> {
			String type = req.header("Accept");
			if (type == null || type.equalsIgnoreCase(MimeType.JSON.toString())) {
				// return gson.toJson(new MyBinaryData(buffer));
				// return JSON.toJSONString(new MyBinaryData(buffer));
				return JSON.toJSONBytes(new MyBinaryData(BYTE_BUFFER));
			} else if (type.equalsIgnoreCase(MimeType.BINARY.toString())) {
				return BYTE_BUFFER;
			}
			return BYTE_BUFFER;
		});

		// On.get(ENDPOINT_BYTE_BUFFER).json(gson.toJson(new
		// MyBinaryData(buffer)));
		// On.get(ENDPOINT_BYTE_BUFFER).contentType(MediaType.APPLICATION_JSON).json(gson.toJson(new
		// MyBinaryData(buffer)));

		// Rapidoid SSE
		// https://github.com/rapidoid/rapidoid/issues/141

		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init rapidoidServer(..)");
		}
	}

	public static void startSparkServer() {
		port(SERVER_SPARK);

		get("/ping", (request, response) -> "pong");

		get(ENDPOINT_HELLO, MimeType.PLAINTEXT.toString(), (req, res) -> "Spark (plain text): Hello World!");
		get("/hello.html", MimeType.HTML.toString(), (req,
				res) -> "<html><header><title>Spark (HTML-style): Hello World!</title></header><body><h1>Hello world!</h1></body></html>");
		get(ENDPOINT_HELLO, MimeType.HTML.toString(), (req,
				res) -> "<html><header><title>Spark (HTML-style): Hello World!</title></header><body><h1>Hello world!</h1></body></html>");
		get(ENDPOINT_HELLO, MimeType.JSON.toString(), (req, res) -> SELF.new MyData("Spark (JSON-style): Hello World!"),
				gson::toJson);
		get(ENDPOINT_HELLO, MimeType.BINARY.toString(), (req, res) -> "Spark (binary): Hello World!".getBytes());

		get("/hello/:name", (req, res) -> "Spark: Hello " + req.params(":name") + "!");

		// simple serialisation logic using FastJson
		get(ENDPOINT_BYTE_BUFFER, MimeType.JSON.toString(), (req, res) -> new MyBinaryData(BYTE_BUFFER),
				JSON::toJSONString);
		get(ENDPOINT_BYTE_BUFFER, MimeType.BINARY.toString(), (req, res) -> BYTE_BUFFER);
		get(ENDPOINT_BYTE_BUFFER, (req, res) -> new MyBinaryData(BYTE_BUFFER), JSON::toJSONString);

//		// byteBuffer with optional path parameters
//		get(ENDPOINT_BYTE_BUFFER + "/:param/:size", (req, res) -> {
//			final String serializerParameter = req.params(":param");
//			final String sizeParameter = req.params(":size");
//
//			SerialiserType type = (serializerParameter == null) ? SerialiserType.BINARY : SerialiserType.parse(serializerParameter);
//			final int size = (sizeParameter == null) ? BYTE_BUFFER.length : Integer.parseInt(sizeParameter);
//			final int reqSize = Math.max(Math.min(size, BYTE_BUFFER.length), 0);
//			if (LOGGER.isDebugEnabled()) {
//				LOGGER.atDebug().addArgument(reqSize).addArgument(size).log("requested size = {} vs input = {}");
//			}
//
//			final String contentType = req.headers("accept");
//			if (contentType != null && contentType.contains(MimeType.BINARY.toString())) {
//				type = SerialiserType.BINARY;
//			} else if (contentType != null && contentType.contains("application/json") && type.isBinary()) {
//				// define default return type for MIME type JSON
//				type = SerialiserType.NATIVE;
//			}
//
//			if (type.isBinary()) {
//				return new MyBinaryData(BYTE_BUFFER, reqSize).data();
//			}
//
//			if (type.equals(SerialiserType.NATIVE)) {
//				return gson.toJson(new MyBinaryData(BYTE_BUFFER, reqSize));
//			}
//			return getJsonMessage(type, new MyBinaryData(BYTE_BUFFER, reqSize));
//		});
//
//		// byteBuffer with optional query parameters
//		get(ENDPOINT_BYTE_BUFFER, (req, res) -> {
//			final String serializerQueryParameter = req.queryParams("serializer");
//			final String sizeQueryParameter = req.queryParams("size");
//			
//			SerialiserType type = (serializerQueryParameter == null) ? SerialiserType.BINARY : SerialiserType.parse(serializerQueryParameter);
//			final int size = (sizeQueryParameter == null) ? BYTE_BUFFER.length : Integer.parseInt(sizeQueryParameter);		
//			final int reqSize = Math.max(Math.min(size, BYTE_BUFFER.length), 0);
//			if (LOGGER.isDebugEnabled()) {
//				LOGGER.atDebug().addArgument(reqSize).addArgument(size).log("requested size = {} vs input = {}");
//			}
//
//			final String contentType = req.headers("accept");
//			if (contentType != null && contentType.contains(MimeType.BINARY.toString())) {
//				type = SerialiserType.BINARY;
//			} else if (contentType != null && contentType.contains("application/json") && type.isBinary()) {
//				// define default return type for MIME type JSON
//				type = SerialiserType.NATIVE;
//			}
//
//			if (type.isBinary()) {
//				return new MyBinaryData(BYTE_BUFFER, reqSize).data();
//			}
//
//			if (type.equals(SerialiserType.NATIVE)) {
//				return gson.toJson(new MyBinaryData(BYTE_BUFFER, reqSize));
//			}
//			return getJsonMessage(type, new MyBinaryData(BYTE_BUFFER, reqSize));
//		});		

		// spark and SSE
		// https://github.com/perwendel/spark/issues/375
		// https://www.smashingmagazine.com/2018/02/sse-websockets-data-flow-http2/
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init sparkServer()");
		}
	}

	public static void startSpringWebFluxServer(final String[] args) {
		new Thread(() -> SELF.new SpringWebfluxApplication().main(args), "webflux").start();
	}

	public static void startTcpServer() {

		ByteBuffer readBuffer = ByteBuffer.allocate(1024);
		HANDLER_TCP_SERVER.setServiceHandler((key, channel) -> {
			readBuffer.clear();
			int numRead = -1;
			numRead = channel.read(readBuffer);
			if (numRead == 0) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.atInfo().log("no data received, server running = " + HANDLER_TCP_SERVER.isRunning());
				}
				return;
			} else if (numRead == -1) {
				SocketAddress remoteAddr = channel.socket().getRemoteSocketAddress();
				if (LOGGER.isInfoEnabled()) {
					LOGGER.atDebug().addArgument(remoteAddr).log("connection closed by client: {}");
				}
				channel.close();
				return;
			}

			String operation = new String(readBuffer.array(), 0, numRead - 1);
			if (operation.toLowerCase(Locale.UK).contains("byte")) {

				channel.write(TCPServer.toByteBuffer(BYTE_BUFFER.length)); // how many bytes to read
				final int numWrite = channel.write(ByteBuffer.wrap(BYTE_BUFFER));
				if (numWrite != BYTE_BUFFER.length) {
					LOGGER.atError().addArgument(numWrite).addArgument(BYTE_BUFFER.length)
							.log("write mismatch: wrote {} bytes should have {} bytes");
				}
			} else {
				// byte[] data = gson.toJson(new MyBinaryData(buffer)).getBytes();
				byte[] data = new MyBinaryData(BYTE_BUFFER).toJson().getBytes();
				channel.write(TCPServer.toByteBuffer(data.length));
				channel.write(ByteBuffer.wrap(data));
			}
		});

		HANDLER_TCP_SERVER.start();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.atInfo().log("init initTcpServer()");
		}
	}

	private static String getJsonMessage(final SerialiserType type, final Object object) {
		switch (type) {
		case GSON:
			return gson.toJson(object);
		case JACKSON:
			try {
				return jackson.writeValueAsString(object);
			} catch (JsonProcessingException e) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.atError().setCause(e).log("JSON Jackson serialiser problem");
				}
			}
			return "";
		case FASTJSON:
			return JSON.toJSONString(object);
		case NATIVE:
		case UNKNOWN:
		default:
			return JSON.toJSONString(object);
		}
	}

	private static MimeType headerMediaType(final Request request) {
		String accept = request.headers("Accept");
		if (accept == null) {
			// TODO: check default implementation (ie. HTML vs. JSON)
			return MimeType.BINARY;
		}

		if (accept.contains(MimeType.BINARY.toString())) {
			return MimeType.BINARY;
		} else if (accept.contains(MimeType.HTML.toString())) {
			return MimeType.HTML;
		} else if (accept.contains(MimeType.JSON.toString())) {
			return MimeType.JSON;
		} else if (accept.contains(MimeType.XML.toString())) {
			return MimeType.XML;
		} else if (accept.contains(MimeType.PLAINTEXT.toString())) {
			return MimeType.PLAINTEXT;
		}

		return MimeType.BINARY;
	}

	public enum MimeType {
		PLAINTEXT("text/plain"), BINARY("application/octet-stream"), JSON("application/json"), XML("text/xml"),
		HTML("application/html"), UNKNOWN("application/octet-stream");

		private final String typeDef;

		private MimeType(String definition) {
			typeDef = definition;
		}

		public static MimeType parse(final String text) {
			if (text == null || text.isEmpty() || text.isBlank()) {
				return UNKNOWN;
			}
			for (MimeType type : MimeType.values()) {
				if (type.toString().equalsIgnoreCase(text)) {
					return type;
				}
			}
			return UNKNOWN;
		}

		@Override
		public String toString() {
			return typeDef;
		}
	}

	@SpringBootApplication
	@EnableAutoConfiguration(exclude = EmbeddedWebServerFactoryCustomizerAutoConfiguration.class)
	@Import({ WebFluxHelloRouter.class, WebFluxByteBufferRouter.class, WebFluxHelloHandler.class,
			WebFluxByteBufferHandler.class })
	public class SpringWebfluxApplication {

		public void main(String[] args) {
			System.setProperty("spring.main.allow-bean-definition-overriding", "true");
			SpringApplication app = new SpringApplication(SpringWebfluxApplication.class);
			app.setDefaultProperties(Collections.singletonMap("server.port", Integer.toString(SERVER_WEBFLUX)));
			app.run(args);

			WebFluxClient gwc = new WebFluxClient();
			LOGGER.atInfo().log(gwc.getTestResult());
			LOGGER.atInfo().log("init springWebFlux(...)");
		}
	}

	@Component
	public class WebFluxByteBufferHandler {
		public Mono<ServerResponse> byteArray(ServerRequest request) {
			List<org.springframework.http.MediaType> acceptedMediaTypes = request.headers().accept();
			if (acceptedMediaTypes.contains(org.springframework.http.MediaType.APPLICATION_JSON)) {
//				return ServerResponse.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
//						.body(Mono.just(gson.toJson(new MyBinaryData(buffer))), String.class);
				return ServerResponse.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.body(Mono.just(JSON.toJSONString(new MyBinaryData(BYTE_BUFFER))), String.class);
			} else if (acceptedMediaTypes.contains(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)) {
				DefaultDataBuffer dataBuffer = FACTORY.wrap(ByteBuffer.wrap(BYTE_BUFFER));
				return ServerResponse.ok().contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
						.body(BodyInserters.fromDataBuffers(Flux.just(dataBuffer)));
			}
			// default return is raw buffer
			DefaultDataBuffer dataBuffer = FACTORY.wrap(ByteBuffer.wrap(BYTE_BUFFER));
			return ServerResponse.ok().contentType(org.springframework.http.MediaType.IMAGE_JPEG)
					.body(BodyInserters.fromDataBuffers(Flux.just(dataBuffer)));
		}
	}

	@Configuration
	public class WebFluxByteBufferRouter {
		@Bean
		public RouterFunction<ServerResponse> byteBufferRoute(WebFluxByteBufferHandler handler) {
			return RouterFunctions.route(GET(ENDPOINT_BYTE_BUFFER), handler::byteArray);
		}
	}

	@Component
	public class WebFluxHelloHandler {
		public Mono<ServerResponse> hello(ServerRequest request) {
			return ServerResponse.ok().contentType(org.springframework.http.MediaType.TEXT_PLAIN)
					.body(BodyInserters.fromObject("Spring: Hello World!"));
		}
	}

	@Configuration
	public class WebFluxHelloRouter {

		@Bean
		public RouterFunction<ServerResponse> helloRoute(WebFluxHelloHandler handler) {
			return RouterFunctions.route(GET(ENDPOINT_HELLO).and(accept(org.springframework.http.MediaType.TEXT_PLAIN)),
					handler::hello);
		}
	}

	class MyData {
		public final String data;

		public MyData(final String data) {
			this.data = data;
		}
	}

}
