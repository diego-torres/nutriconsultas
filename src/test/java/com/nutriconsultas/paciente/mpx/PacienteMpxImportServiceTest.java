package com.nutriconsultas.paciente.mpx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteService;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.calculation.ActivityFactorScale;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.calculation.TefMethod;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class PacienteMpxImportServiceTest {

	private static final String OWNER_USER_ID = "nutritionist-owner";

	private static final Instant FIXED_INSTANT = Instant.parse("2026-06-18T12:00:00Z");

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PacienteService pacienteService;

	private PacienteMpxExportService exportService;

	private PacienteMpxImportService importService;

	private Paciente sourcePaciente;

	@BeforeEach
	void setUp() {
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		importService = new PacienteMpxImportService(pacienteRepository, pacienteService, validator);
		exportService = new PacienteMpxExportService(pacienteRepository, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
		sourcePaciente = buildSamplePaciente();
	}

	@Test
	void importRegistration_roundTripFromExport_createsNewPatientForOwner() {
		when(pacienteRepository.findByIdAndUserId(42L, OWNER_USER_ID))
			.thenReturn(java.util.Optional.of(sourcePaciente));
		when(pacienteRepository.existsByUserIdAndNameIgnoreCaseAndDob(eq(OWNER_USER_ID), eq("Juan Perez"),
				any(Date.class)))
			.thenReturn(false);

		final MpxExportResult export = exportService.exportRegistration(42L, OWNER_USER_ID);
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				export.content());

		final Paciente saved = new Paciente();
		saved.setId(100L);
		when(pacienteService.save(any(Paciente.class))).thenAnswer(invocation -> {
			final Paciente paciente = invocation.getArgument(0);
			saved.setName(paciente.getName());
			saved.setUserId(paciente.getUserId());
			saved.setStatus(paciente.getStatus());
			return saved;
		});

		final MpxImportResult result = importService.importRegistration(file, OWNER_USER_ID);

		assertThat(result.pacienteId()).isEqualTo(100L);
		assertThat(result.duplicateWarning()).isFalse();

		final ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteService).save(captor.capture());
		final Paciente imported = captor.getValue();
		assertThat(imported.getId()).isNull();
		assertThat(imported.getUserId()).isEqualTo(OWNER_USER_ID);
		assertThat(imported.getStatus()).isEqualTo(PacienteStatus.ACTIVE);
		assertThat(imported.getName()).isEqualTo("Juan Perez");
		assertThat(imported.getPatientAuthSub()).isNull();
		assertThat(imported.getAssignedId()).isNull();
		assertThat(imported.getRegistro()).isNotNull();
		assertThat(imported.getNivelPeso()).isEqualTo(NivelPeso.NORMAL);
		assertThat(imported.getEnergyPreferences().getPreferredBmrFormula()).isEqualTo(BmrFormulaType.PROMEDIO);
		assertThat(imported.getMedicalHistory().getTipoSanguineo()).isEqualTo("O+");
	}

	@Test
	void importRegistration_setsDuplicateWarningWhenNameAndDobMatch() {
		final byte[] content = buildMinimalMpxYaml().getBytes(StandardCharsets.UTF_8);
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml", content);
		when(pacienteRepository.existsByUserIdAndNameIgnoreCaseAndDob(any(), any(), any())).thenReturn(true);
		when(pacienteService.save(any(Paciente.class))).thenAnswer(invocation -> {
			final Paciente paciente = invocation.getArgument(0);
			paciente.setId(101L);
			return paciente;
		});

		final MpxImportResult result = importService.importRegistration(file, OWNER_USER_ID);

		assertThat(result.duplicateWarning()).isTrue();
	}

	@Test
	void importRegistration_rejectsUnsupportedMpxVersion() {
		final String yaml = buildMinimalMpxYaml().replace("mpxVersion: 1", "mpxVersion: 2");
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "bad.mpx", "application/x-yaml",
				yaml.getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> importService.importRegistration(file, OWNER_USER_ID))
			.isInstanceOf(MpxImportException.class)
			.hasMessage("Versión MPX no compatible. Solo se admite la versión 1.");
		verify(pacienteService, never()).save(any(Paciente.class));
	}

	@Test
	void importRegistration_rejectsInvalidYaml() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "bad.mpx", "application/x-yaml",
				"not: [valid: yaml".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> importService.importRegistration(file, OWNER_USER_ID))
			.isInstanceOf(MpxImportException.class)
			.hasMessage("El archivo no es un MPX válido");
	}

	@Test
	void importRegistration_rejectsMissingRequiredFields() {
		final String yaml = """
				mpxVersion: 1
				exportedAt: "2026-06-18T12:00:00Z"
				sourceApp: nutriconsultas
				patient:
				  email: juan@example.com
				""";
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "bad.mpx", "application/x-yaml",
				yaml.getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> importService.importRegistration(file, OWNER_USER_ID))
			.isInstanceOf(MpxImportException.class)
			.hasMessageContaining("Datos de paciente inválidos");
	}

	@Test
	void importRegistration_rejectsNonMpxExtension() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.yaml", "application/x-yaml",
				buildMinimalMpxYaml().getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> importService.importRegistration(file, OWNER_USER_ID))
			.isInstanceOf(MpxImportException.class)
			.hasMessage("El archivo debe tener extensión .mpx");
	}

	@Test
	void importRegistration_rejectsEmptyFile() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "empty.mpx", "application/x-yaml", new byte[0]);

		assertThatThrownBy(() -> importService.importRegistration(file, OWNER_USER_ID))
			.isInstanceOf(MpxImportException.class)
			.hasMessage("El archivo está vacío");
	}

	@Test
	void importRegistration_propagatesSubscriptionLimitExceeded() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				buildMinimalMpxYaml().getBytes(StandardCharsets.UTF_8));
		when(pacienteRepository.existsByUserIdAndNameIgnoreCaseAndDob(any(), any(), any())).thenReturn(false);
		when(pacienteService.save(any(Paciente.class)))
			.thenThrow(new SubscriptionLimitExceededException("error.subscription.patient_limit"));

		assertThatThrownBy(() -> importService.importRegistration(file, OWNER_USER_ID))
			.isInstanceOf(SubscriptionLimitExceededException.class);
	}

	@Test
	void importRegistration_assignsAuthenticatedUserIdNotExportTenant() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				buildMinimalMpxYaml().getBytes(StandardCharsets.UTF_8));
		when(pacienteRepository.existsByUserIdAndNameIgnoreCaseAndDob(any(), any(), any())).thenReturn(false);
		when(pacienteService.save(any(Paciente.class))).thenAnswer(invocation -> {
			final Paciente paciente = invocation.getArgument(0);
			paciente.setId(55L);
			return paciente;
		});

		importService.importRegistration(file, "different-nutritionist");

		final ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteService).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo("different-nutritionist");
	}

	private static String buildMinimalMpxYaml() {
		return """
				mpxVersion: 1
				exportedAt: "2026-06-18T12:00:00Z"
				sourceApp: nutriconsultas
				patient:
				  name: Juan Perez
				  dob: "1990-01-15"
				  gender: M
				  pregnancy: false
				""";
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
