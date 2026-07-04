package com.nutriconsultas.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates OpenAI draft-tool arguments against JSON Schema before deserialization
 * (#402). Schemas mirror {@code docs/ai/TOOL-CONTRACT.md} and
 * {@code src/main/resources/ai/schemas/draft-tool-input-schemas.json}.
 */
@Component
@Slf4j
public final class AiDraftToolSchemaValidator {

	static final String SCHEMA_RESOURCE = "ai/schemas/draft-tool-input-schemas.json";

	static final String SCHEMA_DOCUMENT_ID = "https://minutriporcion.local/ai/schemas/draft-tool-input-schemas.json";

	static final String INVALID_JSON_MESSAGE = "Argumentos de herramienta no válidos: JSON mal formado.";

	static final String GENERIC_SCHEMA_MESSAGE = "Argumentos de herramienta no válidos: revisa los campos obligatorios e intenta de nuevo.";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final JsonSchema dishDraftSchema;

	private final JsonSchema menuDraftSchema;

	private final JsonSchema dietPlanDraftSchema;

	public AiDraftToolSchemaValidator() {
		final String schemaContent = loadSchemaContent();
		final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
				builder -> builder.schemaLoaders(
						schemaLoaders -> schemaLoaders.schemas(Map.of(SCHEMA_DOCUMENT_ID, schemaContent))));
		dishDraftSchema = factory.getSchema(SchemaLocation.of(SCHEMA_DOCUMENT_ID + "#/definitions/DishDraftInput"));
		menuDraftSchema = factory.getSchema(SchemaLocation.of(SCHEMA_DOCUMENT_ID + "#/definitions/MenuDraftInput"));
		dietPlanDraftSchema = factory
			.getSchema(SchemaLocation.of(SCHEMA_DOCUMENT_ID + "#/definitions/DietPlanDraftInput"));
	}

	public Optional<String> validateDishDraftArguments(final String argumentsJson) {
		return validate(dishDraftSchema, argumentsJson);
	}

	public Optional<String> validateMenuDraftArguments(final String argumentsJson) {
		return validate(menuDraftSchema, argumentsJson);
	}

	public Optional<String> validateDietPlanDraftArguments(final String argumentsJson) {
		return validate(dietPlanDraftSchema, argumentsJson);
	}

	private static Optional<String> validate(final JsonSchema schema, final String argumentsJson) {
		final JsonNode payload;
		try {
			payload = OBJECT_MAPPER.readTree(argumentsJson);
		}
		catch (IOException ex) {
			return Optional.of(INVALID_JSON_MESSAGE);
		}
		final Set<ValidationMessage> violations = schema.validate(payload);
		if (violations.isEmpty()) {
			return Optional.empty();
		}
		if (log.isWarnEnabled()) {
			log.warn("AI draft tool schema validation failed violationCount={}", violations.size());
		}
		return Optional.of(toSpanishMessage(violations));
	}

	private static String toSpanishMessage(final Set<ValidationMessage> violations) {
		final ValidationMessage first = violations.iterator().next();
		final String keyword = first.getType();
		final String path = formatPath(first.getInstanceLocation().toString());
		if ("required".equals(keyword)) {
			return "Argumentos de herramienta no válidos: falta el campo obligatorio" + path + ".";
		}
		if ("additionalProperties".equals(keyword)) {
			return "Argumentos de herramienta no válidos: campo no permitido" + path + ".";
		}
		if ("minLength".equals(keyword) || "maxLength".equals(keyword)) {
			return "Argumentos de herramienta no válidos: longitud no válida" + path + ".";
		}
		if ("minItems".equals(keyword) || "maxItems".equals(keyword)) {
			return "Argumentos de herramienta no válidos: cantidad de elementos no válida" + path + ".";
		}
		if ("type".equals(keyword)) {
			return "Argumentos de herramienta no válidos: formato incorrecto" + path + ".";
		}
		return GENERIC_SCHEMA_MESSAGE;
	}

	private static String formatPath(final String instanceLocation) {
		if (instanceLocation == null || instanceLocation.isBlank() || "$".equals(instanceLocation)) {
			return "";
		}
		return " (" + instanceLocation.replace("$.", "").replace('/', '.') + ")";
	}

	private static String loadSchemaContent() {
		try (InputStream inputStream = Thread.currentThread()
			.getContextClassLoader()
			.getResourceAsStream(SCHEMA_RESOURCE)) {
			if (inputStream == null) {
				throw new IllegalStateException("Missing draft tool schema resource: " + SCHEMA_RESOURCE);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load draft tool schemas: " + SCHEMA_RESOURCE, ex);
		}
	}

}
