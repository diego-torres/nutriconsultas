package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;

@ExtendWith(MockitoExtension.class)
class MobilePatientDietPlanServiceTest {

	@InjectMocks
	private MobilePatientDietPlanService service;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Test
	void listDietPlans_mapsAssignmentsToSummaryDtos() {
		final PacienteDieta assignment = sampleAssignment(5L, PacienteDietaStatus.ACTIVE, "Plan semanal");
		final Page<PacienteDieta> page = new PageImpl<>(List.of(assignment), PageRequest.of(0, 20), 1);
		when(pacienteDietaRepository.findByPacienteId(eq(1L), any(Pageable.class))).thenReturn(page);

		final PagedResponse<DietPlanSummaryDto> result = service.listDietPlans(1L, 0, 20, false);

		assertThat(result.content()).hasSize(1);
		assertThat(result.content().get(0).assignmentId()).isEqualTo(5L);
		assertThat(result.content().get(0).dietaName()).isEqualTo("Plan semanal");
		assertThat(result.content().get(0).status()).isEqualTo(PacienteDietaStatus.ACTIVE);
		assertThat(result.content().get(0).totalKcal()).isEqualTo(1800);
	}

	@Test
	void listDietPlans_activeOnlyQueriesActiveStatus() {
		when(pacienteDietaRepository.findByPacienteIdAndStatus(eq(1L), eq(PacienteDietaStatus.ACTIVE),
				any(Pageable.class)))
			.thenReturn(Page.empty());

		service.listDietPlans(1L, 0, 20, true);

		verify(pacienteDietaRepository).findByPacienteIdAndStatus(eq(1L), eq(PacienteDietaStatus.ACTIVE),
				any(Pageable.class));
	}

	@Test
	void listDietPlans_capsPageSizeAt100() {
		when(pacienteDietaRepository.findByPacienteId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

		service.listDietPlans(1L, 0, 500, false);

		final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(pacienteDietaRepository).findByPacienteId(eq(1L), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
		assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "startDate"));
	}

	private static PacienteDieta sampleAssignment(final Long id, final PacienteDietaStatus status,
			final String dietaName) {
		final Paciente paciente = new Paciente();
		paciente.setId(1L);
		final Dieta dieta = new Dieta();
		dieta.setId(10L);
		dieta.setNombre(dietaName);
		dieta.setEnergia(1800);
		dieta.setProteina(90.0);
		dieta.setLipidos(60.0);
		dieta.setHidratosDeCarbono(200.0);
		final PacienteDieta assignment = new PacienteDieta();
		assignment.setId(id);
		assignment.setPaciente(paciente);
		assignment.setDieta(dieta);
		assignment.setStatus(status);
		assignment.setStartDate(new Date());
		assignment.setNotes("Notas del plan");
		return assignment;
	}

}
