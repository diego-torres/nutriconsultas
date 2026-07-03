package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Server-injected context for a single orchestration turn (#385).
 */
public record AiOrchestrationContext(String nutritionistId, long threadId,
		@Nullable AiPatientPromptContext patientContext, @Nullable AiDietaPromptContext dietaContext,
		@Nullable AiPlatilloPromptContext platilloContext) {
}
