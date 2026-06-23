package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientOnboardingIntegrationTest {

	private static final String ONBOARDING_SUB = "auth0|mobile-onboarding-profile";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@BeforeEach
	void seedOnboardingPatient() {
		final Paciente existing = pacienteRepository.findByPatientAuthSub(ONBOARDING_SUB).orElse(null);
		if (existing != null) {
			existing.setStatus(PacienteStatus.ONBOARDING);
			existing.setAvatarId(null);
			existing.setName("María López");
			existing.setDisplayName("María");
			pacienteRepository.saveAndFlush(existing);
			return;
		}
		final Paciente paciente = new Paciente();
		paciente.setName("María López");
		paciente.setDisplayName("María");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(ONBOARDING_SUB);
		paciente.setStatus(PacienteStatus.ONBOARDING);
		paciente.setGender("F");
		paciente.setEmail("maria.onboarding@example.com");
		final LocalDate dob = LocalDate.of(1990, 5, 15);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		pacienteRepository.saveAndFlush(paciente);
	}

	@Test
	void getProfile_withOnboardingJwt_returnsProfile() throws Exception {
		mockMvc.perform(get("/rest/mobile/patient/me").with(mobileJwt(ONBOARDING_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("ONBOARDING"))
			.andExpect(jsonPath("$.data.profileComplete").value(false))
			.andExpect(jsonPath("$.data.name").value("María López"));
	}

	@Test
	void patchProfile_withAvatar_transitionsToActive() throws Exception {
		mockMvc
			.perform(patch("/rest/mobile/patient/me").with(mobileJwt(ONBOARDING_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"avatarId\":\"" + PacienteAvatarCatalog.DEFAULT_FEMALE_ID + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.profileComplete").value(true))
			.andExpect(jsonPath("$.data.avatarId").value(PacienteAvatarCatalog.DEFAULT_FEMALE_ID));

		mockMvc.perform(get("/rest/mobile/patient/visits").with(mobileJwt(ONBOARDING_SUB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void patchProfile_withInvalidAvatar_returnsBadRequest() throws Exception {
		mockMvc
			.perform(patch("/rest/mobile/patient/me").with(mobileJwt(ONBOARDING_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"avatarId\":\"invalid-avatar\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Selecciona un avatar válido."));
	}

}
