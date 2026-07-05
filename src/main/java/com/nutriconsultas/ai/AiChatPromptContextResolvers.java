package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;

/**
 * Bundles patient/dieta/platillo prompt context resolvers for {@link AiChatServiceImpl}.
 */
@Component
public final class AiChatPromptContextResolvers {

	private final AiPatientPromptContextResolver patientContextResolver;

	private final AiDietaPromptContextResolver dietaContextResolver;

	private final AiPlatilloPromptContextResolver platilloContextResolver;

	public AiChatPromptContextResolvers(final AiPatientPromptContextResolver patientContextResolver,
			final AiDietaPromptContextResolver dietaContextResolver,
			final AiPlatilloPromptContextResolver platilloContextResolver) {
		this.patientContextResolver = patientContextResolver;
		this.dietaContextResolver = dietaContextResolver;
		this.platilloContextResolver = platilloContextResolver;
	}

	public AiOrchestrationContext buildOrchestrationContext(final String nutritionistId, final long threadId,
			final AiChatPromptContext promptContext) {
		final AiPatientPromptContext patientContext = patientContextResolver
			.resolve(promptContext.patientId(), nutritionistId)
			.orElse(null);
		final AiDietaPromptContext dietaContext = dietaContextResolver.resolve(promptContext.dietaId(), nutritionistId)
			.orElse(null);
		final AiPlatilloPromptContext platilloContext = platilloContextResolver
			.resolve(promptContext.platilloId(), nutritionistId)
			.orElse(null);
		return new AiOrchestrationContext(nutritionistId, threadId, patientContext, dietaContext, platilloContext);
	}

}
