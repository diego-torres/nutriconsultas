package com.nutriconsultas.ai;

import java.util.List;

/**
 * Draft list for a chat thread (#384).
 */
public record AiChatDraftList(long threadId, List<AiChatDraftSummary> drafts) {
}
