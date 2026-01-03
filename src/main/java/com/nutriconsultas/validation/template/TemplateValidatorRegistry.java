package com.nutriconsultas.validation.template;

import java.util.ArrayList;
import java.util.List;

import com.nutriconsultas.alimentos.AlimentoTemplateValidator;
import com.nutriconsultas.calendar.CalendarTemplateValidator;
import com.nutriconsultas.dieta.DietaTemplateValidator;
import com.nutriconsultas.paciente.PacienteTemplateValidator;
import com.nutriconsultas.platillos.PlatilloTemplateValidator;
import com.nutriconsultas.reports.ReportTemplateValidator;
import com.nutriconsultas.search.SearchTemplateValidator;

/**
 * Registry for template validators. Manages all available validators and provides a way
 * to find the appropriate validator for a given template path.
 */
public class TemplateValidatorRegistry {

	private final List<TemplateValidator> validators = new ArrayList<>();

	/**
	 * Creates a new registry with all default validators registered.
	 */
	public TemplateValidatorRegistry() {
		// Register validators in order of specificity (most specific first)
		register(new PacienteTemplateValidator());
		register(new CalendarTemplateValidator());
		register(new PlatilloTemplateValidator());
		register(new DietaTemplateValidator());
		register(new AlimentoTemplateValidator());
		register(new ReportTemplateValidator());
		register(new SearchTemplateValidator());
		register(new EternaTemplateValidator());
		// Default validator should be last (handles "*")
		register(new DefaultTemplateValidator());
	}

	/**
	 * Registers a new validator. Validators are checked in registration order, so more
	 * specific validators should be registered before general ones.
	 * @param validator the validator to register
	 */
	public final void register(final TemplateValidator validator) {
		validators.add(validator);
	}

	/**
	 * Finds the appropriate validator for a given template path. Returns the first
	 * validator that handles the template path.
	 * @param templatePath the template path (e.g., "sbadmin/pacientes/perfil")
	 * @return the validator for this template, or null if none found
	 */
	public TemplateValidator findValidator(final String templatePath) {
		TemplateValidator result = null;
		for (final TemplateValidator validator : validators) {
			if (validator.handlesTemplate(templatePath)) {
				result = validator;
				break;
			}
		}
		return result;
	}

	/**
	 * Gets all registered validators.
	 * @return a list of all validators
	 */
	public List<TemplateValidator> getAllValidators() {
		return new ArrayList<>(validators);
	}

}
