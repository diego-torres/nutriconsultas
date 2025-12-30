package com.nutriconsultas.paciente;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.dieta.Dieta;
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

		// Mock dietas asignadas (used in perfil.html, dietas.html, asignar-dieta.html,
		// editar-dieta.html)
		final List<PacienteDieta> mockDietasAsignadas = new ArrayList<>();
		final PacienteDieta mockPacienteDieta = new PacienteDieta();
		mockPacienteDieta.setId(1L);
		mockPacienteDieta.setPaciente(paciente);
		final Dieta mockDieta = new Dieta();
		mockDieta.setId(1L);
		mockDieta.setNombre("Dieta de Ejemplo");
		// Set mock macronutrientes for template validation
		mockDieta.setProteina(50.0);
		mockDieta.setLipidos(30.0);
		mockDieta.setHidratosDeCarbono(200.0);
		// Calculate and set kilocalorías: protein * 4 + lipids * 9 + carbohydrates * 4
		// 50 * 4 + 30 * 9 + 200 * 4 = 200 + 270 + 800 = 1270 kcal
		mockDieta.setEnergia(1270);
		mockPacienteDieta.setDieta(mockDieta);
		mockPacienteDieta.setStartDate(new Date());
		mockPacienteDieta.setEndDate(null);
		mockPacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);
		mockPacienteDieta.setNotes("");
		mockDietasAsignadas.add(mockPacienteDieta);

		variables.put("dietasAsignadas", mockDietasAsignadas);
		variables.put("dietasActivas", mockDietasAsignadas);
		variables.put("pacienteDieta", mockPacienteDieta);

		// Mock dietas disponibles (used in asignar-dieta.html)
		final List<Dieta> mockDietasDisponibles = new ArrayList<>();
		mockDietasDisponibles.add(mockDieta);
		variables.put("dietasDisponibles", mockDietasDisponibles);

		return variables;
	}

}
