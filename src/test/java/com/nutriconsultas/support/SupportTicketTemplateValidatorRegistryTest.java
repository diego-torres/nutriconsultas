package com.nutriconsultas.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.validation.template.TemplateValidator;
import com.nutriconsultas.validation.template.TemplateValidatorRegistry;

class SupportTicketTemplateValidatorRegistryTest {

	private final TemplateValidatorRegistry registry = new TemplateValidatorRegistry();

	@Test
	void nutritionistSoporteTemplate_usesSupportTicketTemplateValidator() {
		final TemplateValidator validator = registry.findValidator("sbadmin/soporte/listado");

		assertThat(validator).isInstanceOf(SupportTicketTemplateValidator.class);
		assertThat(validator.createMockModelVariables()).containsKeys("tickets", "form", "appVersion");
	}

	@Test
	void adminSoporteTemplate_usesSupportTicketAdminTemplateValidator() {
		final TemplateValidator validator = registry.findValidator("sbadmin/platform/soporte/listado");

		assertThat(validator).isInstanceOf(SupportTicketAdminTemplateValidator.class);
		assertThat(validator.createMockModelVariables()).containsKeys("adminTickets", "estado", "appVersion");
		assertThat(validator.createMockModelVariables().get("platformAdmin")).isEqualTo(true);
	}

}
