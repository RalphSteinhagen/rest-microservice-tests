package ch.steinhagen.rest;

import java.util.Base64;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * 
 * simple binary container class (N.B. thread-unsafe usage of StringBuilder)
 */
class MyBinaryData {
	private static final int BUFFER_SIZE = 4000000;
	private static final StringBuilder STRING_BUILDER = new StringBuilder(BUFFER_SIZE);
	@SerializedName("binaryDataName")
	@JSONField(name = "binaryDataName")
	@JsonProperty("binaryDataName")
	public byte[] binaryData;

	public MyBinaryData() {

	}

	public MyBinaryData(final byte[] data) {
		this.binaryData = data;
	}

	public MyBinaryData(final byte[] data, int size) {
		if (size == data.length) {
			this.binaryData = data;
		} else {
			this.binaryData = new byte[size];
			System.arraycopy(data, 0, this.binaryData, 0, size);
		}
	}

	public byte[] data() {
		return binaryData;
	}

	public String toJson() {
		STRING_BUILDER.setLength(0);
		STRING_BUILDER.append("{\"binaryDataName\":\"");
		STRING_BUILDER.append(Base64.getEncoder().encodeToString(binaryData));
		STRING_BUILDER.append("\"}");
		return STRING_BUILDER.toString();
	}

}