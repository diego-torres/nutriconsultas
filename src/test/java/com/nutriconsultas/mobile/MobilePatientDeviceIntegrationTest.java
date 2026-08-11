package com.nutriconsultas.mobile;

import static com.nutriconsultas.mobile.MobileIntegrationTestJwt.mobileJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.device.PatientDevice;
import com.nutriconsultas.device.PatientDeviceRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobilePatientDeviceIntegrationTest {

	private static final String LINKED_SUB = "auth0|mobile-device-integration";

	private static final String OTHER_SUB = "auth0|mobile-device-other";

	private static final String TOKEN = "fcm-or-apns-token-integration-001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PatientDeviceRepository patientDeviceRepository;

	private Paciente linkedPaciente;

	private Paciente otherPaciente;

	@BeforeEach
	void seedData() {
		linkedPaciente = pacienteRepository.findByPatientAuthSub(LINKED_SUB)
			.orElseGet(() -> pacienteRepository.saveAndFlush(samplePaciente(LINKED_SUB, "Paciente devices")));
		otherPaciente = pacienteRepository.findByPatientAuthSub(OTHER_SUB)
			.orElseGet(() -> pacienteRepository.saveAndFlush(samplePaciente(OTHER_SUB, "Otro paciente devices")));
		patientDeviceRepository.findByPacienteId(linkedPaciente.getId()).forEach(patientDeviceRepository::delete);
		patientDeviceRepository.findByPacienteId(otherPaciente.getId()).forEach(patientDeviceRepository::delete);
		patientDeviceRepository.findByToken(TOKEN).ifPresent(patientDeviceRepository::delete);
		patientDeviceRepository.flush();
	}

	@Test
	void registerDeviceWithLinkedJwtUpsertsAndReturnsSummary() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/devices").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"platform":"IOS","token":"%s","appVersion":"1.2.3"}
						""".formatted(TOKEN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.platform").value("IOS"))
			.andExpect(jsonPath("$.data.updatedAt").exists())
			.andExpect(jsonPath("$.timestamp").exists());

		assertThat(patientDeviceRepository.findByToken(TOKEN)).isPresent();
		assertThat(patientDeviceRepository.findByToken(TOKEN).get().getAppVersion()).isEqualTo("1.2.3");
	}

	@Test
	void registerDeviceAgainRefreshesSameRow() throws Exception {
		registerDevice(LINKED_SUB, TOKEN, "1.0.0");
		final Long firstId = patientDeviceRepository.findByToken(TOKEN).orElseThrow().getId();

		mockMvc
			.perform(post("/rest/mobile/patient/devices").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"platform":"IOS","token":"%s","appVersion":"1.2.4"}
						""".formatted(TOKEN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(firstId))
			.andExpect(jsonPath("$.data.platform").value("IOS"));

		assertThat(patientDeviceRepository.findByToken(TOKEN).orElseThrow().getAppVersion()).isEqualTo("1.2.4");
	}

	@Test
	void registerDeviceReassignsTokenWhenPatientChanges() throws Exception {
		registerDevice(OTHER_SUB, TOKEN, "1.0.0");
		assertThat(patientDeviceRepository.findByToken(TOKEN).orElseThrow().getPaciente().getId())
			.isEqualTo(otherPaciente.getId());

		registerDevice(LINKED_SUB, TOKEN, "2.0.0");

		final PatientDevice device = patientDeviceRepository.findByToken(TOKEN).orElseThrow();
		assertThat(device.getPaciente().getId()).isEqualTo(linkedPaciente.getId());
		assertThat(device.getAppVersion()).isEqualTo("2.0.0");
		assertThat(patientDeviceRepository.findByPacienteId(otherPaciente.getId())).isEmpty();
	}

	@Test
	void deregisterDeviceReturnsNoContentAndIsIdempotent() throws Exception {
		registerDevice(LINKED_SUB, TOKEN, "1.0.0");

		mockMvc
			.perform(delete("/rest/mobile/patient/devices").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s"}
						""".formatted(TOKEN)))
			.andExpect(status().isNoContent());
		assertThat(patientDeviceRepository.findByToken(TOKEN)).isEmpty();

		mockMvc
			.perform(delete("/rest/mobile/patient/devices").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s"}
						""".formatted(TOKEN)))
			.andExpect(status().isNoContent());
	}

	@Test
	void deregisterDeviceDoesNotRemoveOtherPatientsToken() throws Exception {
		registerDevice(OTHER_SUB, TOKEN, "1.0.0");

		mockMvc
			.perform(delete("/rest/mobile/patient/devices").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s"}
						""".formatted(TOKEN)))
			.andExpect(status().isNoContent());

		assertThat(patientDeviceRepository.findByToken(TOKEN)).isPresent();
		assertThat(patientDeviceRepository.findByToken(TOKEN).orElseThrow().getPaciente().getId())
			.isEqualTo(otherPaciente.getId());
	}

	@Test
	void registerDeviceForUnlinkedJwtReturnsForbidden() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/devices").with(mobileJwt("auth0|mobile-device-unlinked"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"platform":"ANDROID","token":"unlinked-token","appVersion":"1.0.0"}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void registerDeviceWithBlankTokenReturnsBadRequest() throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/devices").with(mobileJwt(LINKED_SUB))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"platform":"IOS","token":"   "}
						"""))
			.andExpect(status().isBadRequest());
	}

	private void registerDevice(final String sub, final String token, final String appVersion) throws Exception {
		mockMvc
			.perform(post("/rest/mobile/patient/devices").with(mobileJwt(sub))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"platform":"ANDROID","token":"%s","appVersion":"%s"}
						""".formatted(token, appVersion)))
			.andExpect(status().isOk());
	}

	private static Paciente samplePaciente(final String patientAuthSub, final String name) {
		final Paciente paciente = new Paciente();
		paciente.setName(name);
		paciente.setUserId("auth0|nutritionist-owner");
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setDob(new java.util.Date());
		paciente.setGender("F");
		return paciente;
	}

}
