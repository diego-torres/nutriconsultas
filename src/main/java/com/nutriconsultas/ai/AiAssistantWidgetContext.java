package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Page-scoped context for the floating AI assistant widget (Thymeleaf + JS).
 */
public record AiAssistantWidgetContext(String scopeLabel, @Nullable Long patientId, @Nullable Long dietaId,
		@Nullable Long platilloId) {

	public String storageScopeKey() {
		if (patientId != null) {
			return "patient-" + patientId;
		}
		if (dietaId != null) {
			return "dieta-" + dietaId;
		}
		if (platilloId != null) {
			return "platillo-" + platilloId;
		}
		return "general";
	}

}
