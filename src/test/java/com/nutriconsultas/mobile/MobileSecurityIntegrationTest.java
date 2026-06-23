package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileSecurityIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-security-linked";

	private static final String ONBOARDING_SUB = "auth0|mobile-security-onboarding";

	private static final String REVOKED_SUB = "auth0|mobile-security-revoked";

	private static final String UNLINKED_SUB = "auth0|mobile-security-unlinked";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@BeforeEach
	void seedPatients() {
		SecurityContextHolder.clearContext();
		ensurePatient(LINKED_SUB, PacienteStatus.ACTIVE);
		ensurePatient(ONBOARDING_SUB, PacienteStatus.ONBOARDING);
		ensurePatient(REVOKED_SUB, PacienteStatus.REVOKED);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void patientEndpoint_withoutJwt_returnsUnauthorized() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits")).andExpect(status().isUnauthorized());
	}

	@Test
	void patientEndpoint_withUnlinkedJwt_returnsOnboardingRequired() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(UNLINKED_SUB)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Completa el registro para acceder a este recurso."))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void patientEndpoint_withUnlinkedJwt_returnsEnglishOnboardingMessageWhenRequested() throws Exception {
		mockMvc
			.perform(get("/rest/mobile/patient/visits").with(mobileJwt(UNLINKED_SUB)).header("Accept-Language", "en"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Complete onboarding to access this resource."));
	}

	@Test
	void patientEndpoint_withOnboardingJwtOnDataEndpoint_returnsOnboardingRequired() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(ONBOARDING_SUB)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Completa el registro para acceder a este recurso."));
	}

	@Test
	void patientEndpoint_withRevokedJwt_returnsOnboardingRequired() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(REVOKED_SUB)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Completa el registro para acceder a este recurso."));
	}

	@Test
	void patientEndpoint_withLinkedJwt_passesLinkageFilter() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(LINKED_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void nonPatientMobilePath_doesNotRequirePatientLinkage() throws Exception {
		mockMvc.perform(get("/rest/mobile/status").with(mobileJwt(UNLINKED_SUB))).andExpect(status().isNotFound());
	}

	private void ensurePatient(final String patientAuthSub, final PacienteStatus status) {
		final Paciente existing = pacienteRepository.findByPatientAuthSub(patientAuthSub).orElse(null);
		if (existing != null) {
			if (existing.getStatus() != status) {
				existing.setStatus(status);
				pacienteRepository.saveAndFlush(existing);
			}
			return;
		}
		final Paciente paciente = samplePaciente(patientAuthSub);
		paciente.setStatus(status);
		pacienteRepository.saveAndFlush(paciente);
	}

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Mobile Security Patient");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(patientAuthSub);
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
