package com.nutriconsultas.paciente;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

@ExtendWith(MockitoExtension.class)
class PacienteProfileReportModelAdviceTest {

	@InjectMocks
	private PacienteProfileReportModelAdvice advice;

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Mock
	private Model model;

	@Mock
	private org.springframework.security.oauth2.core.oidc.user.OidcUser principal;

	@Test
	void addReportEntitlementFlagsAddsPdfAndFullReportFlags() {
		when(principal.getSubject()).thenReturn("user-123");
		when(subscriptionEntitlementService.hasEntitlement("user-123", Entitlement.PDF_EXPORT)).thenReturn(true);
		when(subscriptionEntitlementService.hasEntitlement("user-123", Entitlement.REPORTS_FULL)).thenReturn(false);

		advice.addReportEntitlementFlags(principal, model);

		verify(model).addAttribute("canExportPdf", true);
		verify(model).addAttribute("canFullReports", false);
	}

	@Test
	void addReportEntitlementFlagsSkipsWhenPrincipalMissing() {
		advice.addReportEntitlementFlags(null, model);

		verifyNoInteractions(model);
		verifyNoInteractions(subscriptionEntitlementService);
	}

}
