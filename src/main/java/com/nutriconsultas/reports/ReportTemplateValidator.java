package com.nutriconsultas.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.anthropometric.BodyMass;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for patient progress report templates. Provides mock variables for report
 * generation including patient data, consultations, measurements, exams, and dietary
 * plans.
 */
public class ReportTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/reports/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();

		// Create mock paciente
		final Paciente mockPaciente = createMockPaciente();
		variables.put("paciente", mockPaciente);

		// Create mock consultations
		final List<CalendarEvent> mockConsultations = createMockConsultations(mockPaciente);
		variables.put("consultations", mockConsultations);

		// Create mock measurements
		final List<AnthropometricMeasurement> mockMeasurements = createMockMeasurements(mockPaciente);
		variables.put("measurements", mockMeasurements);

		// Create mock exams
		final List<ClinicalExam> mockExams = createMockExams(mockPaciente);
		variables.put("exams", mockExams);

		// Create mock dietary plans
		final List<PacienteDieta> mockDietaryPlans = createMockDietaryPlans(mockPaciente);
		variables.put("dietaryPlans", mockDietaryPlans);

		// Create mock weight/BMI trend
		final List<PatientReportService.WeightBmiDataPoint> mockWeightBmiTrend = createMockWeightBmiTrend();
		variables.put("weightBmiTrend", mockWeightBmiTrend);

		// Create date variables
		final Date now = new Date();
		// 30 days ago
		final long thirtyDaysAgo = now.getTime() - 30L * 24 * 60 * 60 * 1000;
		variables.put("startDate", new Date(thirtyDaysAgo));
		variables.put("endDate", now);
		variables.put("reportDate", now);

		// Create mock pacientes list for listado template
		final List<Paciente> mockPacientes = new ArrayList<>();
		mockPacientes.add(mockPaciente);
		variables.put("pacientes", mockPacientes);

		// Create mock nutrition analysis result for nutrition-analysis template
		final NutritionAnalysisResult mockAnalysis = createMockNutritionAnalysis();
		variables.put("analysis", mockAnalysis);

		return variables;
	}

	private Paciente createMockPaciente() {
		final Paciente paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Paciente de Prueba");
		paciente.setUserId("user123");
		// 30 years ago
		final long thirtyYearsAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000 * 30;
		paciente.setDob(new Date(thirtyYearsAgo));
		paciente.setGender("M");
		paciente.setEmail("paciente@example.com");
		paciente.setPhone("1234567890");
		paciente.setPeso(70.0);
		paciente.setEstatura(1.75);
		paciente.setImc(22.86);
		paciente.setResponsibleName("Responsable");
		paciente.setParentesco("Padre");
		paciente.setAntecedentesPatologicosPersonales("Antecedentes personales de prueba");
		paciente.setAntecedentesPatologicosFamiliares("Antecedentes familiares de prueba");
		paciente.setAlergias("Alergias de prueba");
		paciente.setHipertension(false);
		paciente.setDiabetes(false);
		paciente.setHipotiroidismo(false);
		paciente.setObesidad(false);
		return paciente;
	}

	private List<CalendarEvent> createMockConsultations(final Paciente paciente) {
		final List<CalendarEvent> consultations = new ArrayList<>();
		final CalendarEvent consultation = new CalendarEvent();
		consultation.setId(1L);
		consultation.setPaciente(paciente);
		consultation.setEventDateTime(new Date());
		consultation.setTitle("Consulta de seguimiento");
		consultation.setDurationMinutes(30);
		consultation.setStatus(EventStatus.COMPLETED);
		consultation.setPeso(70.5);
		consultation.setImc(23.0);
		consultations.add(consultation);
		return consultations;
	}

	private List<AnthropometricMeasurement> createMockMeasurements(final Paciente paciente) {
		final List<AnthropometricMeasurement> measurements = new ArrayList<>();
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(1L);
		measurement.setPaciente(paciente);
		measurement.setMeasurementDateTime(new Date());
		measurement.setTitle("Medición Antropométrica");
		final BodyMass bodyMass = new BodyMass();
		bodyMass.setWeight(70.0);
		bodyMass.setHeight(1.75);
		bodyMass.setImc(22.86);
		measurement.setBodyMass(bodyMass);
		measurements.add(measurement);
		return measurements;
	}

	private List<ClinicalExam> createMockExams(final Paciente paciente) {
		final List<ClinicalExam> exams = new ArrayList<>();
		final ClinicalExam exam = new ClinicalExam();
		exam.setId(1L);
		exam.setPaciente(paciente);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");
		exam.setPeso(70.0);
		exams.add(exam);
		return exams;
	}

	private List<PacienteDieta> createMockDietaryPlans(final Paciente paciente) {
		final List<PacienteDieta> plans = new ArrayList<>();
		final PacienteDieta plan = new PacienteDieta();
		plan.setId(1L);
		plan.setPaciente(paciente);
		// 7 days ago
		final long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
		plan.setStartDate(new Date(sevenDaysAgo));
		plan.setStatus(PacienteDietaStatus.ACTIVE);
		plan.setNotes("Plan dietético de prueba");
		plans.add(plan);
		return plans;
	}

	private List<PatientReportService.WeightBmiDataPoint> createMockWeightBmiTrend() {
		final List<PatientReportService.WeightBmiDataPoint> trend = new ArrayList<>();
		final PatientReportService.WeightBmiDataPoint point = new PatientReportService.WeightBmiDataPoint();
		point.setDate(new Date());
		point.setWeight(70.0);
		point.setBmi(22.86);
		point.setSource("Consulta");
		trend.add(point);
		return trend;
	}

	private NutritionAnalysisResult createMockNutritionAnalysis() {
		final NutritionAnalysisResult analysis = new NutritionAnalysisResult();

		// Create dieta summary
		final NutritionAnalysisResult.DietaSummary dietaSummary = new NutritionAnalysisResult.DietaSummary();
		dietaSummary.setId(1L);
		dietaSummary.setNombre("Dieta de Prueba");
		analysis.setDieta(dietaSummary);

		// Create nutrient totals
		final NutritionAnalysisResult.NutrientTotals totals = new NutritionAnalysisResult.NutrientTotals();
		totals.setEnergia(2000);
		totals.setProteina(60.0);
		totals.setLipidos(70.0);
		totals.setHidratosDeCarbono(320.0);
		totals.setFibra(30.0);
		totals.setVitA(1000.0);
		totals.setAcidoAscorbico(100.0);
		totals.setAcidoFolico(450.0);
		totals.setCalcio(1100.0);
		totals.setHierro(20.0);
		totals.setSodio(2000.0);
		totals.setPotasio(3600.0);
		totals.setFosforo(750.0);
		totals.setSelenio(60.0);
		totals.setColesterol(250.0);
		totals.setAgSaturados(15.0);
		totals.setAzucarPorEquivalente(40.0);
		analysis.setTotals(totals);

		// Create distribution
		final NutritionAnalysisResult.NutrientDistribution distribution = new NutritionAnalysisResult.NutrientDistribution();
		distribution.setProteinPercentage(23.0);
		distribution.setLipidsPercentage(31.0);
		distribution.setCarbohydratesPercentage(46.0);
		analysis.setDistribution(distribution);

		// Create some deficiencies
		final List<NutritionAnalysisResult.NutrientDeficiency> deficiencies = new ArrayList<>();
		final NutritionAnalysisResult.NutrientDeficiency deficiency = new NutritionAnalysisResult.NutrientDeficiency();
		deficiency.setNutrientName("Hierro");
		deficiency.setActualValue(10.0);
		deficiency.setRecommendedValue(18.0);
		deficiency.setUnit("mg");
		deficiency.setPercentageOfRDV(55.6);
		deficiency.setRecommendation("Incluya carnes magras, legumbres y espinacas en su dieta.");
		deficiencies.add(deficiency);
		analysis.setDeficiencies(deficiencies);

		// Create some excesses
		final List<NutritionAnalysisResult.NutrientExcess> excesses = new ArrayList<>();
		final NutritionAnalysisResult.NutrientExcess excess = new NutritionAnalysisResult.NutrientExcess();
		excess.setNutrientName("Sodio");
		excess.setActualValue(2500.0);
		excess.setRecommendedValue(2300.0);
		excess.setUnit("mg");
		excess.setPercentageOfRDV(108.7);
		excess.setRecommendation("Reduzca el consumo de alimentos procesados y sal.");
		excesses.add(excess);
		analysis.setExcesses(excesses);

		// Create recommendations
		final List<String> recommendations = new ArrayList<>();
		recommendations.add("Se identificaron 1 nutrientes por debajo de los valores recomendados.");
		recommendations.add("Se identificaron 1 nutrientes que exceden los valores recomendados.");
		analysis.setRecommendations(recommendations);

		return analysis;
	}

}
