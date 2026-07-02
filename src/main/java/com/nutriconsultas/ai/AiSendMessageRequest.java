package com.nutriconsultas.ai;

/**
 * Request body for {@code POST /rest/nutritionist/ai/chat/message} (#384).
 */
public record AiSendMessageRequest(long threadId, String message) {
}
