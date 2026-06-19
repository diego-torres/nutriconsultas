package com.nutriconsultas.paciente.mpx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.TefMethod;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class PacienteMpxExportServiceTest {

	private static final String OWNER_USER_ID = "nutritionist-owner";

	private static final String OTHER_USER_ID = "nutritionist-other";

	private static final Instant FIXED_INSTANT = Instant.parse("2026-06-18T12:00:00Z");

	@Mock
	private PacienteRepository pacienteRepository;

	private PacienteMpxExportService service;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		service = new PacienteMpxExportService(pacienteRepository, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
		paciente = buildSamplePaciente();
	}

	@Test
	void exportRegistration_containsExpectedRegistrationFields() {
		when(pacienteRepository.findByIdAndUserId(42L, OWNER_USER_ID)).thenReturn(Optional.of(paciente));

		final MpxExportResult result = service.exportRegistration(42L, OWNER_USER_ID);
		final String yaml = new String(result.content(), StandardCharsets.UTF_8);

		assertThat(result.filename()).endsWith(".mpx");
		assertThat(result.filename()).startsWith("mp-001-");
		assertThat(yaml).contains("mpxVersion: 1");
		assertThat(yaml).contains("sourceApp: nutriconsultas");
		assertThat(yaml).contains("exportedAt:");
		assertThat(yaml).contains("2026-06-18T12:00:00Z");
		assertThat(yaml).contains("name: Juan Perez");
		assertThat(yaml).contains("dob:");
		assertThat(yaml).contains("1990-01-15");
		assertThat(yaml).contains("gender: M");
		assertThat(yaml).contains("tipoSanguineo: O+");
		assertThat(yaml).contains("preferredBmrFormula: PROMEDIO");
		assertThat(yaml).contains("nivelPeso: NORMAL");
		assertThat(yaml).contains("pregnancy: false");
	}

	@Test
	void exportRegistration_excludesTenantAndHistoryIdentifiers() {
		when(pacienteRepository.findByIdAndUserId(42L, OWNER_USER_ID)).thenReturn(Optional.of(paciente));

		final MpxExportResult result = service.exportRegistration(42L, OWNER_USER_ID);
		final String yaml = new String(result.content(), StandardCharsets.UTF_8);

		assertThat(yaml).doesNotContain("userId:");
		assertThat(yaml).doesNotContain("patientAuthSub:");
		assertThat(yaml).doesNotContain("registro:");
		assertThat(yaml).doesNotContain("assignedId:");
		assertThat(yaml).doesNotContain("status:");
		assertThat(yaml).doesNotContain("id:");
	}

	@Test
	void exportRegistration_throwsWhenPatientNotFoundForOwner() {
		when(pacienteRepository.findByIdAndUserId(99L, OWNER_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.exportRegistration(99L, OWNER_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Paciente no encontrado");
	}

	@Test
	void exportRegistration_throwsWhenPatientOwnedByAnotherNutritionist() {
		when(pacienteRepository.findByIdAndUserId(42L, OTHER_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.exportRegistration(42L, OTHER_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Paciente no encontrado");
	}

	private static Paciente buildSamplePaciente() {
		final Paciente entity = new Paciente();
		entity.setId(42L);
		entity.setUserId(OWNER_USER_ID);
		entity.setPatientAuthSub("auth0|patient-sub");
		entity.setAssignedId("MP-001");
		entity.setName("Juan Perez");
		entity.setEmail("juan@example.com");
		entity.setPhone("5551234");
		entity.setGender("M");
		entity.setResponsibleName("Maria Perez");
		entity.setParentesco("Madre");
		entity.setPregnancy(false);
		entity.setDob(Date.from(LocalDate.of(1990, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		entity.setRegistro(Date.from(Instant.parse("2025-01-01T10:00:00Z")));

		entity.setPeso(72.5);
		entity.setEstatura(1.75);
		entity.setImc(23.7);
		entity.setBmr(1650.0);
		entity.setGetKcal(2275.0);
		entity.setNivelPeso(NivelPeso.NORMAL);
		entity.setTefKcal(182.0);
		entity.setTotalAdjustedKcal(2457.0);
		entity.setStressKcal(0.0);
		entity.setFinalTotalKcal(2457.0);

		entity.getEnergyPreferences().setActivityFactorScale(ActivityFactorScale.HARRIS_BENEDICT);
		entity.getEnergyPreferences().setPreferredBmrFormula(BmrFormulaType.PROMEDIO);
		entity.getEnergyPreferences().setPhysicalActivityLevel(PhysicalActivityLevel.MODERATE);
		entity.getEnergyPreferences().setActivityFactor(1.55);
		entity.getEnergyPreferences().setTefMethod(TefMethod.FIXED);

		entity.getMedicalHistory().setTipoSanguineo("O+");
		entity.getMedicalHistory().setAntecedentesPatologicosPersonales("Ninguno");
		entity.getMedicalHistory().setHipertension(false);
		entity.getMedicalHistory().setDiabetes(false);

		return entity;
	}

}
