package com.nutriconsultas.ai;

import java.util.List;

/**
 * Outcome of truncating a thread and resubmitting an edited user message (#437).
 */
public record AiEditResubmitResult(AiOrchestrationResult orchestration, int truncatedMessageCount,
		List<Long> discardedDraftIds) {

}
