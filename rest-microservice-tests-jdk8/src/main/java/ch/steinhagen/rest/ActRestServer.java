package ch.steinhagen.rest;

import act.Act;
import act.app.ActionContext;
import act.controller.Controller;
import act.inject.DefaultValue;

import java.util.Base64;

import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

public class ActRestServer { // NOPMD - nomen est omen
	private static final Logger LOGGER = LoggerFactory.getLogger(ActRestServer.class);
	private static byte[] byteBuffer = new byte[1000000];
	{
		for (int i = 0; i < byteBuffer.length; i++) {
			byteBuffer[i] = (byte) (65 + i % 128);
		}
	}
	private static Gson gson = initGson();
	private static ObjectMapper jackson = new ObjectMapper();

	@GetAction
	public String hello(@DefaultValue("World") String who) {
		return "Hello " + who + "!";
	}

	@GetAction("/hello")
	public void sayHello(@DefaultValue("World") String who) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.atDebug().addArgument(who).log("Hello argument = {}");
		}
		Controller.Util.render(who);
	}

	@GetAction("/bye")
	public String sayBye() {
		return "Bye!";
	}

	// GET action accessible via e.g. "[..]/byteBuffer/gson/10" or
	// "[..]/byteBuffer?serialiser=gson&size=10"
	// default return is binary and full buffer length (default size parameter <->
	// -1)
	@GetAction("/byteBuffer/{serialiser}/{size}")
	public Result byteBufferWithURLParameter(@DefaultValue("application/octet-stream") final String serialiser,
			@DefaultValue("-1") final int size) {
		SerialiserType type = SerialiserType.parse(serialiser);
		final int reqSize = (size == -1) ? byteBuffer.length : Math.max(Math.min(size, byteBuffer.length), 0);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.atDebug().addArgument(reqSize).addArgument(size).log("requested size = {} vs input = {}");
		}
		
		// check additionally for MIME type in header
		final String contentType = ActionContext.current().req().header("accept").toLowerCase();
		if (contentType != null && contentType.contains("application/octet-stream")) {
			type = SerialiserType.BINARY;
		} else if (contentType != null && contentType.contains("application/json") && type.isBinary()) {
			// define default return type for MIME type JSON
			type = SerialiserType.NATIVE;
		}
		
		if (type.isBinary()) {
			return Controller.Util.renderBinary(new MyBinaryData(byteBuffer, reqSize).data());
		}
		
		if (type.equals(SerialiserType.NATIVE)) {
            return Controller.Util.renderJson(new MyBinaryData(byteBuffer, reqSize));
        }
        return Controller.Util.renderText(getJsonMessage(type, new MyBinaryData(byteBuffer, reqSize)));
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
			return Controller.Util.renderJson(object).toString();
		}
	}

	public static void main(String[] args) throws Exception {
		LOGGER.atInfo().addArgument(System.getProperty("java.version")).log("java version = {}");
		System.setProperty("http.port", Integer.toString(8085));
		System.setProperty("http.port", Integer.toString(8085));
		Act.start("ActRestServer");
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
}
