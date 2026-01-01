package com.nutriconsultas.paciente;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.ClinicalExam;
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
		final Map<String, Object> variables = super.createMockModelVariables();

		final Paciente paciente = createMockPaciente();
		variables.put("paciente", paciente);

		variables.put("citaAnterior", "");
		variables.put("citaSiguiente", "");

		addMockDietas(variables, paciente);
		addMockConsulta(variables, paciente);
		addMockClinicos(variables, paciente);
		addMockExam(variables, paciente);

		return variables;
	}

	private Paciente createMockPaciente() {
		final Paciente paciente = new Paciente();
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
		return paciente;
	}

	private void addMockDietas(final Map<String, Object> variables, final Paciente paciente) {
		final List<PacienteDieta> mockDietasAsignadas = new ArrayList<>();
		final PacienteDieta mockPacienteDieta = new PacienteDieta();
		mockPacienteDieta.setId(1L);
		mockPacienteDieta.setPaciente(paciente);
		final Dieta mockDieta = new Dieta();
		mockDieta.setId(1L);
		mockDieta.setNombre("Dieta de Ejemplo");
		mockDieta.setProteina(50.0);
		mockDieta.setLipidos(30.0);
		mockDieta.setHidratosDeCarbono(200.0);
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

		final List<Dieta> mockDietasDisponibles = new ArrayList<>();
		mockDietasDisponibles.add(mockDieta);
		variables.put("dietasDisponibles", mockDietasDisponibles);
	}

	private void addMockConsulta(final Map<String, Object> variables, final Paciente paciente) {
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
	}

	private void addMockClinicos(final Map<String, Object> variables, final Paciente paciente) {
		final ClinicalExam mockClinicos = createMockClinicalExam(0L, paciente);
		variables.put("clinicos", mockClinicos);
	}

	private void addMockExam(final Map<String, Object> variables, final Paciente paciente) {
		final ClinicalExam mockExam = createMockClinicalExam(1L, paciente);
		variables.put("exam", mockExam);
	}

	private ClinicalExam createMockClinicalExam(final Long id, final Paciente paciente) {
		final ClinicalExam exam = new ClinicalExam();
		exam.setId(id);
		exam.setExamDateTime(new Date());
		exam.setPaciente(paciente);
		exam.setTitle("Examen Clínico");
		exam.setDescription("");
		exam.setSummaryNotes("");
		exam.setPeso(null);
		exam.setEstatura(null);
		exam.setImc(null);
		exam.setSistolica(null);
		exam.setDiastolica(null);
		exam.setPulso(null);
		exam.setIndiceGlucemico(null);
		exam.setSpo2(null);
		exam.setTemperatura(null);
		exam.setHdl(null);
		exam.setLdl(null);
		exam.setTrigliceridos(null);
		exam.setColesterolTotal(null);
		exam.setGlucosa(null);
		exam.setHba1c(null);
		exam.setCreatinina(null);
		exam.setUrea(null);
		exam.setBun(null);
		exam.setAlt(null);
		exam.setAst(null);
		exam.setBilirrubina(null);
		exam.setHemoglobina(null);
		exam.setHematocrito(null);
		exam.setLeucocitos(null);
		exam.setPlaquetas(null);
		exam.setVitaminaD(null);
		exam.setVitaminaB12(null);
		exam.setHierro(null);
		exam.setFerritina(null);
		return exam;
	}

}
