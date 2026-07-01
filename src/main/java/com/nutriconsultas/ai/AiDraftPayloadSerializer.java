package com.nutriconsultas.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serializes AI draft payloads to JSON for persistence.
 */
public final class AiDraftPayloadSerializer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private AiDraftPayloadSerializer() {
	}

	public static String toJson(final Object payload) {
		try {
			return OBJECT_MAPPER.writeValueAsString(payload);
		}
		catch (JsonProcessingException ex) {
			throw new AiDraftLifecycleException("No se pudo serializar el borrador.", ex);
		}
	}

}
