package com.nutriconsultas.ai;

import java.util.Optional;

import org.springframework.lang.Nullable;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AiDietaPromptContextResolver {

	Optional<AiDietaPromptContext> resolve(@Nullable Long dietaId, String nutritionistId);

}
