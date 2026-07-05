package com.nutriconsultas.ai;

/**
 * Centralized Spanish user-facing messages for AI chat errors (#399).
 */
public final class AiErrorMessages {

	public static final String GENERIC = "No se pudo completar la solicitud.";

	public static final String MISCONFIGURATION = "El asistente de IA no está disponible en este momento. "
			+ "Contacta al administrador del sistema.";

	public static final String NOT_CONFIGURED = "El asistente de IA no está disponible en este momento.";

	public static final String OPENAI_AUTH = "No se pudo autenticar con el servicio de IA. Contacta al administrador.";

	public static final String OPENAI_RATE_LIMIT = "El servicio de IA está saturado. Intenta de nuevo en unos minutos.";

	public static final String OPENAI_MODEL_NOT_FOUND = "El modelo de IA configurado no está disponible. "
			+ "Contacta al administrador.";

	public static final String OPENAI_INVALID_REQUEST = "No se pudo procesar la solicitud de IA. "
			+ "Intenta reformular tu mensaje.";

	public static final String OPENAI_UNAVAILABLE = "El servicio de IA no está disponible temporalmente. "
			+ "Intenta más tarde.";

	public static final String OPENAI_UNKNOWN = "Ocurrió un error al comunicarse con el servicio de IA.";

	public static final String OPENAI_TIMEOUT = "El servicio de IA tardó demasiado en responder. Intenta de nuevo.";

	public static final String RATE_LIMIT = "Has alcanzado el límite de mensajes del asistente de IA. "
			+ "Intenta de nuevo en unos minutos.";

	public static final String EMPTY_MESSAGE = "El mensaje no puede estar vacío.";

	public static final String INVALID_REQUEST = "Solicitud no válida.";

	public static final String INVALID_SESSION = "Sesión no válida.";

	public static final String THREAD_UPDATE_FAILED = "No se pudo actualizar la conversación.";

	private AiErrorMessages() {
	}

	public static AiToolErrorCode errorCodeForOpenAi(final OpenAiClientException.ErrorKind kind) {
		return switch (kind) {
			case RATE_LIMIT -> AiToolErrorCode.RATE_LIMIT;
			case NOT_CONFIGURED, UNAVAILABLE, TIMEOUT, AUTH, MODEL_NOT_FOUND -> AiToolErrorCode.INTERNAL;
			default -> AiToolErrorCode.VALIDATION;
		};
	}

}
