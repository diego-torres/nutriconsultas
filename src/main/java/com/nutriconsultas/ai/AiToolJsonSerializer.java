package com.nutriconsultas.ai;

import java.util.List;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serializes AI tool results to JSON for OpenAI tool responses.
 */
public final class AiToolJsonSerializer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private AiToolJsonSerializer() {
	}

	public static String toJson(final Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw new AiOrchestrationException("No se pudo serializar la respuesta de la herramienta.", ex);
		}
	}

	public static JsonNode parseJson(final String json) {
		try {
			return OBJECT_MAPPER.readTree(json);
		}
		catch (JsonProcessingException ex) {
			throw new AiOrchestrationException("Argumentos de herramienta no válidos.", ex);
		}
	}

	public static <T> T fromJson(final String json, final Class<T> type) {
		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JsonProcessingException ex) {
			throw new AiOrchestrationException("Argumentos de herramienta no válidos.", ex);
		}
	}

	public static <T> T convert(final JsonNode node, final Class<T> type) {
		try {
			return OBJECT_MAPPER.treeToValue(node, type);
		}
		catch (JsonProcessingException ex) {
			throw new AiOrchestrationException("Argumentos de herramienta no válidos.", ex);
		}
	}

	public static <T> List<T> convertList(final JsonNode arrayNode, final Class<T> elementType) {
		try {
			return OBJECT_MAPPER.readerForListOf(elementType).readValue(arrayNode);
		}
		catch (IOException ex) {
			throw new AiOrchestrationException("Argumentos de herramienta no válidos.", ex);
		}
	}

}
