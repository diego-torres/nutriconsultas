package com.nutriconsultas.ai;

import java.util.Optional;

import org.springframework.lang.Nullable;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AiPlatilloPromptContextResolver {

	Optional<AiPlatilloPromptContext> resolve(@Nullable Long platilloId, String nutritionistId);

}
