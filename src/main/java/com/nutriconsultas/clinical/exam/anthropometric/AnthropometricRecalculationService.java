package com.nutriconsultas.clinical.exam.anthropometric;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.calculation.BmrCalculationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies recalculation groups when an anthropometric field is corrected (#242).
 */
@Service
@Slf4j
public class AnthropometricRecalculationService {

	private final BodyCompositionService bodyCompositionService;

	private final SomatotypeService somatotypeService;

	private final CalendarEventService calendarEventService;

	private final ClinicalExamService clinicalExamService;

	private final AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	private final PacienteRepository pacienteRepository;

	public AnthropometricRecalculationService(final BodyCompositionService bodyCompositionService,
			final SomatotypeService somatotypeService, final CalendarEventService calendarEventService,
			final ClinicalExamService clinicalExamService,
			final AnthropometricMeasurementRepository anthropometricMeasurementRepository,
			final PacienteRepository pacienteRepository) {
		this.bodyCompositionService = bodyCompositionService;
		this.somatotypeService = somatotypeService;
		this.calendarEventService = calendarEventService;
		this.clinicalExamService = clinicalExamService;
		this.anthropometricMeasurementRepository = anthropometricMeasurementRepository;
		this.pacienteRepository = pacienteRepository;
	}

	@Transactional
	public AnthropometricDerivedFieldsDto applyRecalcGroups(final AnthropometricMeasurement measurement,
			final Set<AnthropometricRecalcGroup> groups, final Double correctedFieldValue) {
		final Paciente paciente = measurement.getPaciente();
		Double imc = measurement.getImc();

		if (groups.contains(AnthropometricRecalcGroup.BMI)) {
			imc = recalculateBmi(measurement);
		}
		if (groups.contains(AnthropometricRecalcGroup.COMPOSITION)) {
			bodyCompositionService.applyToMeasurement(measurement, paciente, imc);
		}
		if (groups.contains(AnthropometricRecalcGroup.SOMATOTYPE)) {
			somatotypeService.applyToMeasurement(measurement, paciente);
		}
		if (groups.contains(AnthropometricRecalcGroup.PATIENT_SNAPSHOT)) {
			updatePatientSnapshotIfNeeded(paciente, measurement, imc, measurement.getNivelPeso());
		}

		return toDerivedFields(measurement, correctedFieldValue);
	}

	public AnthropometricDerivedFieldsDto toDerivedFields(final AnthropometricMeasurement measurement,
			final Double correctedFieldValue) {
		return new AnthropometricDerivedFieldsDto(correctedFieldValue, measurement.getImc(), measurement.getNivelPeso(),
				measurement.getPorcentajeGrasaCorporal(), measurement.getPorcentajeMasaMuscular(),
				measurement.getMasaOseaKg(), measurement.getPorcentajeMasaOsea(), measurement.getEndomorphy(),
				measurement.getMesomorphy(), measurement.getEctomorphy(), measurement.getBmrUsed(),
				measurement.getGetKcal(), measurement.getFinalTotalKcal(), measurement.getUpdatedAt());
	}

	public Set<AnthropometricRecalcGroup> groupsForField(final AnthropometricFieldDefinition definition) {
		return definition.recalcGroups() != null ? definition.recalcGroups()
				: EnumSet.noneOf(AnthropometricRecalcGroup.class);
	}

	private Double recalculateBmi(final AnthropometricMeasurement measurement) {
		final Double imc = AnthropometricBmiCalculator.calculateImc(measurement.getPeso(), measurement.getEstatura());
		if (imc != null) {
			measurement.setImc(imc);
			measurement.setNivelPeso(AnthropometricBmiCalculator.calculateNivelPeso(imc));
		}
		else {
			measurement.setImc(null);
			measurement.setNivelPeso(null);
		}
		return imc;
	}

	private void updatePatientSnapshotIfNeeded(final Paciente paciente, final AnthropometricMeasurement measurement,
			final Double imc, final NivelPeso nivelPeso) {
		if (paciente == null || measurement.getMeasurementDateTime() == null) {
			return;
		}
		final Date measurementDate = measurement.getMeasurementDateTime();
		final LocalDate today = LocalDate.now();
		final LocalDate measurementLocalDate = convertDateToLocalDate(measurementDate);

		if (today.equals(measurementLocalDate)) {
			log.debug("Updating patient snapshot from today's anthropometric correction, pacienteId={}",
					paciente.getId());
			updatePatientFromMeasurement(paciente, measurement, imc, nivelPeso);
			return;
		}

		if (isLatestMeasurement(paciente.getId(), measurementDate)) {
			log.debug("Updating patient snapshot from latest anthropometric correction, pacienteId={}",
					paciente.getId());
			updatePatientFromMeasurement(paciente, measurement, imc, nivelPeso);
		}
	}

	private boolean isLatestMeasurement(final Long pacienteId, final Date measurementDate) {
		final List<CalendarEvent> eventos = calendarEventService.findByPacienteId(pacienteId);
		final List<ClinicalExam> examenes = clinicalExamService.findByPacienteId(pacienteId);
		final List<AnthropometricMeasurement> mediciones = anthropometricMeasurementRepository
			.findByPacienteId(pacienteId);
		return eventos.stream()
			.noneMatch(event -> event.getEventDateTime() != null && event.getEventDateTime().after(measurementDate))
				&& examenes.stream()
					.noneMatch(exam -> exam.getExamDateTime() != null && exam.getExamDateTime().after(measurementDate))
				&& mediciones.stream()
					.noneMatch(item -> item.getMeasurementDateTime() != null
							&& item.getMeasurementDateTime().after(measurementDate));
	}

	private void updatePatientFromMeasurement(final Paciente paciente, final AnthropometricMeasurement measurement,
			final Double imc, final NivelPeso nivelPeso) {
		if (measurement.getPeso() != null) {
			paciente.setPeso(measurement.getPeso());
		}
		if (measurement.getEstatura() != null) {
			paciente.setEstatura(measurement.getEstatura());
		}
		if (imc != null) {
			paciente.setImc(imc);
		}
		if (nivelPeso != null) {
			paciente.setNivelPeso(nivelPeso);
		}
		refreshPatientBmrIfPossible(paciente);
		pacienteRepository.save(paciente);
	}

	private void refreshPatientBmrIfPossible(final Paciente paciente) {
		if (paciente.getPeso() == null || paciente.getEstatura() == null) {
			return;
		}
		final Integer age = calculateAge(paciente.getDob());
		final Boolean isMale = "M".equalsIgnoreCase(paciente.getGender());
		final Double bmr = BmrCalculationService.calculatePromedioBmr(paciente.getPeso(), paciente.getEstatura(), age,
				isMale);
		if (bmr != null) {
			paciente.setBmr(bmr);
		}
	}

	private Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		final LocalDate birthDate = convertDateToLocalDate(dob);
		return Period.between(birthDate, LocalDate.now()).getYears();
	}

	private LocalDate convertDateToLocalDate(final Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

}
