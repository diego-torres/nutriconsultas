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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileSecurityIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-security-linked";

	private static final String UNLINKED_SUB = "auth0|mobile-security-unlinked";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@BeforeEach
	void seedLinkedPatient() {
		SecurityContextHolder.clearContext();
		if (pacienteRepository.findByPatientAuthSub(LINKED_SUB).isEmpty()) {
			pacienteRepository.saveAndFlush(samplePaciente(LINKED_SUB));
		}
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
	void patientEndpoint_withUnlinkedJwt_returnsForbidden() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(UNLINKED_SUB)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("La cuenta del paciente no está vinculada."))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void patientEndpoint_withUnlinkedJwt_returnsEnglishMessageWhenRequested() throws Exception {
		mockMvc
			.perform(get("/rest/mobile/patient/visits").with(mobileJwt(UNLINKED_SUB)).header("Accept-Language", "en"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Patient account is not linked."));
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
