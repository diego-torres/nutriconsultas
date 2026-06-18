package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.paciente.PacienteService;
import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

	private static final String USER_ID = "auth0|user-1";

	@InjectMocks
	private ReportController reportController;

	@Mock
	private PacienteService pacienteService;

	@Mock
	private DietaRepository dietaRepository;

	@Mock
	private ClinicStatisticsService clinicStatisticsService;

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Mock
	private SubscriptionErrorResponses subscriptionErrorResponses;

	@Mock
	private OidcUser principal;

	@Test
	void listadoExposesEntitlementFlagsForBasicoUser() {
		when(principal.getSubject()).thenReturn(USER_ID);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_BASIC)).thenReturn(true);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.PDF_EXPORT)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_ADVANCED)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_FULL)).thenReturn(false);
		when(pacienteService.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
		when(dietaRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

		final Model model = new ExtendedModelMap();
		final String view = reportController.listado(model, principal);

		assertThat(view).isEqualTo("sbadmin/reports/listado");
		assertThat(model.getAttribute("canExportPdf")).isEqualTo(false);
		assertThat(model.getAttribute("canAdvancedReports")).isEqualTo(false);
		assertThat(model.getAttribute("canFullReports")).isEqualTo(false);
	}

	@Test
	void listadoBlocksWhenReportsBasicUnavailable() {
		when(principal.getSubject()).thenReturn(USER_ID);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_BASIC)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.PDF_EXPORT)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_ADVANCED)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_FULL)).thenReturn(false);

		final Model model = new ExtendedModelMap();
		final String view = reportController.listado(model, principal);

		assertThat(view).isEqualTo("sbadmin/reports/listado");
		assertThat(model.getAttribute("error"))
			.isEqualTo("Los reportes no están disponibles con tu suscripción actual.");
	}

	@Test
	void estadisticasBlocksWhenAdvancedReportsUnavailable() {
		when(principal.getSubject()).thenReturn(USER_ID);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.PDF_EXPORT)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_ADVANCED)).thenReturn(false);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_FULL)).thenReturn(false);
		when(subscriptionErrorResponses.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("advanced denied");

		final Model model = new ExtendedModelMap();
		final String view = reportController.estadisticas(model, principal, null, null);

		assertThat(view).isEqualTo("sbadmin/reports/estadisticas");
		assertThat(model.getAttribute("error")).isEqualTo("advanced denied");
		assertThat(model.getAttribute("statistics")).isNull();
	}

	@Test
	void estadisticasLoadsStatisticsForProfesionalUser() {
		when(principal.getSubject()).thenReturn(USER_ID);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.PDF_EXPORT)).thenReturn(true);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_ADVANCED)).thenReturn(true);
		when(subscriptionEntitlementService.hasEntitlement(USER_ID, Entitlement.REPORTS_FULL)).thenReturn(true);
		final ClinicStatistics statistics = new ClinicStatistics();
		statistics.setTotalPatients(3L);
		when(clinicStatisticsService.generateStatistics(USER_ID, null, null)).thenReturn(statistics);

		final Model model = new ExtendedModelMap();
		final String view = reportController.estadisticas(model, principal, null, null);

		assertThat(view).isEqualTo("sbadmin/reports/estadisticas");
		assertThat(model.getAttribute("statistics")).isSameAs(statistics);
		verify(clinicStatisticsService).generateStatistics(USER_ID, null, null);
	}

}
