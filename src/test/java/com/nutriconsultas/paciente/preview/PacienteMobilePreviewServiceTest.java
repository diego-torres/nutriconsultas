package com.nutriconsultas.paciente.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.mobile.MobilePatientDietPlanService;
import com.nutriconsultas.mobile.MobilePatientProgressService;
import com.nutriconsultas.mobile.MobilePatientVisitService;
import com.nutriconsultas.mobile.dto.DietIngestaDto;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteService;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;

@ExtendWith(MockitoExtension.class)
class PacienteMobilePreviewServiceTest {

	private static final String USER_ID = "auth0|nutritionist-preview";

	@InjectMocks
	private PacienteMobilePreviewService service;

	@Mock
	private PacienteService pacienteService;

	@Mock
	private MobilePatientProgressService mobilePatientProgressService;

	@Mock
	private MobilePatientDietPlanService mobilePatientDietPlanService;

	@Mock
	private MobilePatientVisitService mobilePatientVisitService;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Test
	void buildPreview_returnsAggregatedPayload() {
		final Paciente paciente = paciente(10L, "María López", "María");
		when(pacienteService.findByIdAndUserId(10L, USER_ID)).thenReturn(paciente);

		final PatientProgressSnapshotDto progress = new PatientProgressSnapshotDto(Instant.now(), null, 72.5, 1.68,
				25.7, NivelPeso.ALTO, "Sobrepeso", 1450.0, null, -0.8, -0.3, null, "avatar_1", null);
		when(mobilePatientProgressService.getSnapshot(10L)).thenReturn(progress);

		final DietPlanSummaryDto summary = new DietPlanSummaryDto(55L, PacienteDietaStatus.ACTIVE,
				LocalDate.of(2026, 3, 16), null, null, "Plan hipocalórico", 1800, 90.0, 55.0, 200.0);
		when(mobilePatientDietPlanService.listDietPlans(eq(10L), eq(0), eq(1), eq(true)))
			.thenReturn(new PagedResponse<>(List.of(summary), 0, 1, 1, 1, true));

		final DietPlanDetailDto detail = new DietPlanDetailDto(55L, PacienteDietaStatus.ACTIVE,
				LocalDate.of(2026, 3, 16), null, null, "Plan hipocalórico", 1800, 90.0, 55.0, 200.0,
				List.of(new DietIngestaDto("Desayuno", 450, 20.0, 10.0, 50.0, List.of(), List.of())));
		when(mobilePatientDietPlanService.getDietPlanDetail(10L, 55L)).thenReturn(detail);

		final Instant soon = Instant.now().plus(2, ChronoUnit.DAYS);
		final VisitSummaryDto visit = new VisitSummaryDto(7L, soon, "Consulta seguimiento", EventStatus.SCHEDULED, 60,
				null);
		when(mobilePatientVisitService.listVisits(eq(10L), eq(0), eq(20), eq(EventStatus.SCHEDULED), any(Instant.class),
				isNull()))
			.thenReturn(new PagedResponse<>(List.of(visit), 0, 20, 1, 1, true));

		final NutritionistProfile profile = new NutritionistProfile();
		profile.setDisplayName("Lic. Ana López");
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

		final PatientMobilePreviewDto preview = service.buildPreview(10L, USER_ID);

		assertThat(preview.firstName()).isEqualTo("María");
		assertThat(preview.nutritionistDisplayName()).isEqualTo("Lic. Ana López");
		assertThat(preview.progress()).isEqualTo(progress);
		assertThat(preview.activePlan()).isEqualTo(summary);
		assertThat(preview.activePlanDetail()).isEqualTo(detail);
		assertThat(preview.nextVisit()).isEqualTo(visit);
		assertThat(preview.activePlanDetail().ingestas()).hasSize(1);
	}

	@Test
	void buildPreview_withoutDietOrVisit_returnsNullSections() {
		final Paciente paciente = paciente(11L, "Juan Perez", null);
		when(pacienteService.findByIdAndUserId(11L, USER_ID)).thenReturn(paciente);
		when(mobilePatientProgressService.getSnapshot(11L)).thenReturn(new PatientProgressSnapshotDto(null, null, null,
				null, null, null, null, null, null, null, null, null, null, null));
		when(mobilePatientDietPlanService.listDietPlans(eq(11L), eq(0), eq(1), eq(true)))
			.thenReturn(new PagedResponse<>(List.of(), 0, 1, 0, 0, true));
		when(mobilePatientVisitService.listVisits(eq(11L), eq(0), eq(20), eq(EventStatus.SCHEDULED), any(Instant.class),
				isNull()))
			.thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true));
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

		final PatientMobilePreviewDto preview = service.buildPreview(11L, USER_ID);

		assertThat(preview.firstName()).isEqualTo("Juan");
		assertThat(preview.activePlan()).isNull();
		assertThat(preview.activePlanDetail()).isNull();
		assertThat(preview.nextVisit()).isNull();
		assertThat(preview.nutritionistDisplayName()).isNull();
		verify(mobilePatientDietPlanService, never()).getDietPlanDetail(any(), any());
	}

	@Test
	void buildPreview_throwsWhenPatientNotOwned() {
		when(pacienteService.findByIdAndUserId(99L, USER_ID)).thenReturn(null);

		assertThatThrownBy(() -> service.buildPreview(99L, USER_ID)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Paciente no encontrado");
		verify(mobilePatientProgressService, never()).getSnapshot(any());
		verify(mobilePatientDietPlanService, never()).listDietPlans(any(), anyInt(), anyInt(), anyBoolean());
	}

	@Test
	void resolveFirstName_fallsBackToPaciente() {
		final Paciente paciente = paciente(1L, null, null);
		assertThat(PacienteMobilePreviewService.resolveFirstName(paciente)).isEqualTo("Paciente");
	}

	@Test
	void pickSoonestUpcomingVisit_selectsEarliestFuture() {
		final Instant now = Instant.parse("2026-08-11T12:00:00Z");
		final VisitSummaryDto later = new VisitSummaryDto(2L, now.plus(5, ChronoUnit.DAYS), "Later",
				EventStatus.SCHEDULED, 30, null);
		final VisitSummaryDto sooner = new VisitSummaryDto(1L, now.plus(1, ChronoUnit.DAYS), "Sooner",
				EventStatus.SCHEDULED, 30, null);
		final VisitSummaryDto past = new VisitSummaryDto(3L, now.minus(1, ChronoUnit.DAYS), "Past",
				EventStatus.SCHEDULED, 30, null);

		assertThat(PacienteMobilePreviewService.pickSoonestUpcomingVisit(List.of(later, sooner, past), now))
			.isEqualTo(sooner);
	}

	private static Paciente paciente(final Long id, final String name, final String displayName) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setName(name);
		paciente.setDisplayName(displayName);
		paciente.setUserId(USER_ID);
		return paciente;
	}

}
