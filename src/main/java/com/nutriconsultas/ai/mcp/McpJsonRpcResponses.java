package com.nutriconsultas.ai.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-RPC 2.0 response helpers for MCP (#394).
 */
public final class McpJsonRpcResponses {

	public static final String PROTOCOL_VERSION = "2025-03-26";

	public static final int ERROR_UNAUTHORIZED = -32_001;

	public static final int ERROR_INVALID_REQUEST = -32_600;

	public static final int ERROR_METHOD_NOT_FOUND = -32_601;

	public static final int ERROR_FORBIDDEN = -32_003;

	public static final int ERROR_NOT_FOUND = -32_004;

	public static final int ERROR_UNAVAILABLE = -32_005;

	public static final int ERROR_SERVER = -32_000;

	private McpJsonRpcResponses() {
	}

	public static Map<String, Object> success(final Object id, final Map<String, Object> result) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("jsonrpc", "2.0");
		body.put("id", id);
		body.put("result", result);
		return body;
	}

	public static Map<String, Object> error(final Object id, final int code, final String message) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("jsonrpc", "2.0");
		body.put("id", id);
		final Map<String, Object> error = new LinkedHashMap<>();
		error.put("code", code);
		error.put("message", message);
		body.put("error", error);
		return body;
	}

	public static Map<String, Object> toolCallResult(final Object id, final String toolResultJson,
			final boolean isError) {
		final Map<String, Object> content = Map.of("type", "text", "text", toolResultJson);
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("content", List.of(content));
		result.put("isError", isError);
		return success(id, result);
	}

}
