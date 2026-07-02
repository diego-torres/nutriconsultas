package com.nutriconsultas.ai;

import java.util.Optional;

import org.springframework.lang.Nullable;

/**
 * Builds redacted patient prompt context for owned patients (#384, #367).
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AiPatientPromptContextResolver {

	Optional<AiPatientPromptContext> resolve(@Nullable Long patientId, String nutritionistId);

}
