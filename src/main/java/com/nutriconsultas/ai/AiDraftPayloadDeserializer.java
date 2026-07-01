package com.nutriconsultas.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Deserializes persisted AI draft JSON payloads.
 */
public final class AiDraftPayloadDeserializer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private AiDraftPayloadDeserializer() {
	}

	public static DishDraftPayload dish(final String jsonPayload) {
		return read(jsonPayload, DishDraftPayload.class);
	}

	public static MenuDraftPayload menu(final String jsonPayload) {
		return read(jsonPayload, MenuDraftPayload.class);
	}

	public static DietPlanDraftPayload dietPlan(final String jsonPayload) {
		return read(jsonPayload, DietPlanDraftPayload.class);
	}

	private static <T> T read(final String jsonPayload, final Class<T> type) {
		try {
			return OBJECT_MAPPER.readValue(jsonPayload, type);
		}
		catch (JsonProcessingException ex) {
			throw new AiDraftLifecycleException("No se pudo leer el borrador.", ex);
		}
	}

}
