package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.paciente.projection.PacienteCalendarView;
import com.nutriconsultas.paciente.projection.PacienteListView;

@DataJpaTest
@ActiveProfiles("test")
class PacienteProjectionRepositoryTest {

	private static final String USER_ID = "nutritionist-projection-test";

	private static final String PATIENT_SUB = "auth0|projection-patient";

	@Autowired
	private PacienteRepository pacienteRepository;

	@BeforeEach
	void seedPaciente() {
		pacienteRepository.deleteAll();
		pacienteRepository.save(samplePaciente());
	}

	@Test
	void findListViewsByUserId_returnsGridFieldsWithoutLoadingFullEntity() {
		final Page<PacienteListView> page = pacienteRepository.findListViewsByUserId(USER_ID, PageRequest.of(0, 10));

		assertThat(page.getContent()).hasSize(1);
		final PacienteListView view = page.getContent().get(0);
		assertThat(view.getName()).isEqualTo("Projection Grid Patient");
		assertThat(view.getEmail()).isEqualTo("grid@example.com");
		assertThat(view.getPhone()).isEqualTo("5551234");
		assertThat(view.getGender()).isEqualTo("F");
		assertThat(view.getResponsibleName()).isEqualTo("Tutor Test");
		assertThat(view.getStatus()).isEqualTo(PacienteStatus.ACTIVE);
		assertThat(view.getPatientAuthSub()).isEqualTo(PATIENT_SUB);
	}

	@Test
	void findListViewsByUserIdAndSearchTerm_matchesNameEmailOrPhone() {
		final Page<PacienteListView> byEmail = pacienteRepository.findListViewsByUserIdAndSearchTerm(USER_ID,
				"%grid@example%", PageRequest.of(0, 10));
		final List<PacienteListView> byPhone = pacienteRepository.findListViewsByUserIdAndSearchTerm(USER_ID,
				"%5551234");

		assertThat(byEmail.getContent()).hasSize(1);
		assertThat(byPhone).hasSize(1);
	}

	@Test
	void findAuthViewByPatientAuthSub_returnsIdentityFields() {
		final Optional<PacienteAuthView> authView = pacienteRepository.findAuthViewByPatientAuthSub(PATIENT_SUB);

		assertThat(authView).isPresent();
		assertThat(authView.orElseThrow().getUserId()).isEqualTo(USER_ID);
		assertThat(authView.orElseThrow().getPatientAuthSub()).isEqualTo(PATIENT_SUB);
	}

	@Test
	void findCalendarViewsByUserId_returnsCalendarDropdownFields() {
		final List<PacienteCalendarView> views = pacienteRepository.findCalendarViewsByUserId(USER_ID);

		assertThat(views).hasSize(1);
		assertThat(views.get(0).getName()).isEqualTo("Projection Grid Patient");
		assertThat(views.get(0).getGender()).isEqualTo("F");
		assertThat(views.get(0).getDob()).isNotNull();
	}

	private static Paciente samplePaciente() {
		final Paciente paciente = new Paciente();
		paciente.setName("Projection Grid Patient");
		paciente.setUserId(USER_ID);
		paciente.setPatientAuthSub(PATIENT_SUB);
		paciente.setEmail("grid@example.com");
		paciente.setPhone("5551234");
		paciente.setGender("F");
		paciente.setResponsibleName("Tutor Test");
		paciente.setAntecedentesPatologicosPersonales("Heavy TEXT antecedentes should not be required for list views");
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return paciente;
	}

}
