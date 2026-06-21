package com.nutriconsultas.clinical.exam.anthropometric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AnthropometricRecalculationServiceTest {

	@InjectMocks
	private AnthropometricRecalculationService service;

	@Mock
	private BodyCompositionService bodyCompositionService;

	@Mock
	private SomatotypeService somatotypeService;

	@Mock
	private CalendarEventService calendarEventService;

	@Mock
	private ClinicalExamService clinicalExamService;

	@Mock
	private AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	private AnthropometricMeasurement measurement;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setUserId("user-1");
		measurement = new AnthropometricMeasurement();
		measurement.setId(10L);
		measurement.setPaciente(paciente);
		measurement.setPeso(80.0);
		measurement.setEstatura(1.75);
	}

	@Test
	void applyRecalcGroupsRecalculatesBmi() {
		final AnthropometricDerivedFieldsDto result = service.applyRecalcGroups(measurement,
				EnumSet.of(AnthropometricRecalcGroup.BMI), 80.0);

		assertThat(measurement.getImc()).isEqualTo(26.122448979591837);
		assertThat(measurement.getNivelPeso()).isEqualTo(NivelPeso.ALTO);
		assertThat(result.imc()).isEqualTo(measurement.getImc());
	}

	@Test
	void applyRecalcGroupsInvokesCompositionAndSomatotype() {
		service.applyRecalcGroups(measurement,
				EnumSet.of(AnthropometricRecalcGroup.COMPOSITION, AnthropometricRecalcGroup.SOMATOTYPE), 12.0);

		verify(bodyCompositionService).applyToMeasurement(eq(measurement), eq(paciente), any());
		verify(somatotypeService).applyToMeasurement(measurement, paciente);
	}

	@Test
	void applyRecalcGroupsUpdatesPatientSnapshotWhenLatest() {
		when(pacienteRepository.save(paciente)).thenReturn(paciente);

		measurement.setMeasurementDateTime(new java.util.Date());

		service.applyRecalcGroups(measurement,
				EnumSet.of(AnthropometricRecalcGroup.BMI, AnthropometricRecalcGroup.PATIENT_SNAPSHOT), 80.0);

		assertThat(paciente.getPeso()).isEqualTo(80.0);
		verify(pacienteRepository).save(paciente);
	}

}
