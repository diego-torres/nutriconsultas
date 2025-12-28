package com.nutriconsultas.paciente;

import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for paciente (patient) templates. Provides mock variables for patient
 * profile, consultation, and related pages.
 */
public class PacienteTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/pacientes/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		Map<String, Object> variables = super.createMockModelVariables();

		// Create a new Paciente object with default values
		Paciente paciente = new Paciente();
		paciente.setId(0L);
		paciente.setName("");
		paciente.setEmail("");
		paciente.setPhone("");
		paciente.setGender("M");
		paciente.setPeso(null);
		paciente.setEstatura(null);
		paciente.setImc(null);
		paciente.setResponsibleName("");
		paciente.setParentesco("");
		paciente.setTipoSanguineo("");
		paciente.setAntecedentesPrenatales("");
		paciente.setAntecedentesNatales("");
		paciente.setAntecedentesPatologicosPersonales("");
		paciente.setAntecedentesPatologicosFamiliares("");
		paciente.setComplicaciones("");
		paciente.setHistorialAlimenticio("");
		paciente.setDesarrolloPsicomotor("");
		paciente.setAlergias("");
		paciente.setEnfermedadesHepaticas(false);
		paciente.setHipertension(false);
		paciente.setDiabetes(false);
		paciente.setHipotiroidismo(false);
		paciente.setObesidad(false);
		paciente.setAnemia(false);
		paciente.setBulimia(false);
		paciente.setAnorexia(false);

		variables.put("paciente", paciente);

		// Mock citaAnterior and citaSiguiente (used in perfil.html)
		variables.put("citaAnterior", "");
		variables.put("citaSiguiente", "");

		return variables;
	}

}
