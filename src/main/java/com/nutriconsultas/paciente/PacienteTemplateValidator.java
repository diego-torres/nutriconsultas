package com.nutriconsultas.paciente;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.EventStatus;
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

		// Mock consulta CalendarEvent (used in consulta.html)
		final CalendarEvent mockConsulta = new CalendarEvent();
		mockConsulta.setId(0L);
		mockConsulta.setEventDateTime(new Date());
		mockConsulta.setPaciente(paciente);
		mockConsulta.setTitle("Consulta");
		mockConsulta.setDurationMinutes(60);
		mockConsulta.setStatus(EventStatus.SCHEDULED);
		mockConsulta.setDescription("");
		mockConsulta.setSummaryNotes("");
		mockConsulta.setPeso(null);
		mockConsulta.setEstatura(null);
		mockConsulta.setImc(null);
		mockConsulta.setSistolica(null);
		mockConsulta.setDiastolica(null);
		mockConsulta.setPulso(null);
		mockConsulta.setIndiceGlucemico(null);
		mockConsulta.setSpo2(null);
		mockConsulta.setTemperatura(null);
		variables.put("consulta", mockConsulta);

		// Mock clinicos ClinicalExam (used in clinicos.html)
		final ClinicalExam mockClinicos = new ClinicalExam();
		mockClinicos.setId(0L);
		mockClinicos.setExamDateTime(new Date());
		mockClinicos.setPaciente(paciente);
		mockClinicos.setTitle("Examen Clínico");
		mockClinicos.setDescription("");
		mockClinicos.setSummaryNotes("");
		mockClinicos.setPeso(null);
		mockClinicos.setEstatura(null);
		mockClinicos.setImc(null);
		mockClinicos.setSistolica(null);
		mockClinicos.setDiastolica(null);
		mockClinicos.setPulso(null);
		mockClinicos.setIndiceGlucemico(null);
		mockClinicos.setSpo2(null);
		mockClinicos.setTemperatura(null);
		// Lipid profile
		mockClinicos.setHdl(null);
		mockClinicos.setLdl(null);
		mockClinicos.setTrigliceridos(null);
		mockClinicos.setColesterolTotal(null);
		// Blood chemistry
		mockClinicos.setGlucosa(null);
		mockClinicos.setHba1c(null);
		mockClinicos.setCreatinina(null);
		mockClinicos.setUrea(null);
		mockClinicos.setBun(null);
		// Liver function
		mockClinicos.setAlt(null);
		mockClinicos.setAst(null);
		mockClinicos.setBilirrubina(null);
		// Complete blood count
		mockClinicos.setHemoglobina(null);
		mockClinicos.setHematocrito(null);
		mockClinicos.setLeucocitos(null);
		mockClinicos.setPlaquetas(null);
		// Other tests
		mockClinicos.setVitaminaD(null);
		mockClinicos.setVitaminaB12(null);
		mockClinicos.setHierro(null);
		mockClinicos.setFerritina(null);
		variables.put("clinicos", mockClinicos);

		// Mock exam ClinicalExam (used in ver-examen-clinico.html)
		final ClinicalExam mockExam = new ClinicalExam();
		mockExam.setId(1L);
		mockExam.setExamDateTime(new Date());
		mockExam.setPaciente(paciente);
		mockExam.setTitle("Examen Clínico");
		mockExam.setDescription("");
		mockExam.setSummaryNotes("");
		mockExam.setPeso(null);
		mockExam.setEstatura(null);
		mockExam.setImc(null);
		mockExam.setSistolica(null);
		mockExam.setDiastolica(null);
		mockExam.setPulso(null);
		mockExam.setIndiceGlucemico(null);
		mockExam.setSpo2(null);
		mockExam.setTemperatura(null);
		// Lipid profile
		mockExam.setHdl(null);
		mockExam.setLdl(null);
		mockExam.setTrigliceridos(null);
		mockExam.setColesterolTotal(null);
		// Blood chemistry
		mockExam.setGlucosa(null);
		mockExam.setHba1c(null);
		mockExam.setCreatinina(null);
		mockExam.setUrea(null);
		mockExam.setBun(null);
		// Liver function
		mockExam.setAlt(null);
		mockExam.setAst(null);
		mockExam.setBilirrubina(null);
		// Complete blood count
		mockExam.setHemoglobina(null);
		mockExam.setHematocrito(null);
		mockExam.setLeucocitos(null);
		mockExam.setPlaquetas(null);
		// Other tests
		mockExam.setVitaminaD(null);
		mockExam.setVitaminaB12(null);
		mockExam.setHierro(null);
		mockExam.setFerritina(null);
		variables.put("exam", mockExam);

		return variables;
	}

}
