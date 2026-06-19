package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionAdminRestControllerTest {

	@Mock
	private SubscriptionGridService gridService;

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private SubscriptionOwnerResolver ownerResolver;

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@Mock
	private OidcUser principal;

	@InjectMocks
	private SubscriptionAdminRestController controller;

	@BeforeEach
	void setupSecurityContext() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal);
		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	void getPageArray_returnsServerSideRows() {
		final Subscription subscription = new Subscription();
		subscription.setId(2L);
		subscription.setPlanTier(PlanTier.PROFESIONAL);
		subscription.setStatus(SubscriptionStatus.TRIAL);
		subscription.setPaymentExempt(true);
		subscription.setPeriodEnd(Instant.parse("2026-07-17T16:39:26Z"));
		when(gridService.findPage(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(subscription)));
		when(gridService.countAll()).thenReturn(1L);
		final Clinic clinic = new Clinic();
		clinic.setName("Minutriporción");
		when(clinicRepository.findBySubscriptionId(2L)).thenReturn(Optional.of(clinic));
		when(ownerResolver.resolve(2L))
			.thenReturn(Optional.of(new SubscriptionOwnerView("nutri@example.com", "auth0|owner-1", 6L)));
		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setDraw(1);
		pagingRequest.setStart(0);
		pagingRequest.setLength(25);

		final PageArray pageArray = controller.getPageArray(pagingRequest);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "subscriptions.list");
		assertThat(pageArray.getRecordsTotal()).isEqualTo(1);
		assertThat(pageArray.getRecordsFiltered()).isEqualTo(1);
		assertThat(pageArray.getData()).hasSize(1);
		assertThat(pageArray.getData().get(0).get(0)).isEqualTo("2");
		assertThat(pageArray.getData().get(0).get(1)).isEqualTo("nutri@example.com");
		assertThat(pageArray.getData().get(0).get(2)).isEqualTo("Minutriporción");
		assertThat(pageArray.getData().get(0).get(3)).isEqualTo("PROFESIONAL");
	}

	@Test
	void getPageArray_whenSubscriptionCancelled_showsRevokedLabelInsteadOfEditLink() {
		final Subscription subscription = new Subscription();
		subscription.setId(4L);
		subscription.setPlanTier(PlanTier.BASICO);
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		when(gridService.findPage(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(subscription)));
		when(gridService.countAll()).thenReturn(1L);
		when(ownerResolver.resolve(4L)).thenReturn(Optional.empty());
		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setDraw(1);
		pagingRequest.setStart(0);
		pagingRequest.setLength(25);

		final PageArray pageArray = controller.getPageArray(pagingRequest);

		assertThat(pageArray.getData().get(0).get(7)).contains("Revocada");
	}

}
