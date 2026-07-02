package com.nutriconsultas.ai;

import java.time.Instant;

/**
 * User-visible chat message for REST responses (#384).
 */
public record AiChatMessageView(long id, AiChatMessageRole role, String content, Instant createdAt) {
}
