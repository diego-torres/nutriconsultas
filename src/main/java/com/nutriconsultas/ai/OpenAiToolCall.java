package com.nutriconsultas.ai;

/**
 * Tool call requested by the assistant model (#366).
 */
public record OpenAiToolCall(String id, String name, String argumentsJson) {

}
