package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

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
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationGridService;

@ExtendWith(MockitoExtension.class)
class NutritionistInvitationRestControllerTest {

	@Mock
	private NutritionistInvitationGridService gridService;

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@Mock
	private OidcUser principal;

	@InjectMocks
	private NutritionistInvitationRestController controller;

	@BeforeEach
	void setupSecurityContext() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal);
		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	void getPageArray_returnsServerSideRows() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(5L);
		invitation.setEmail("nutri@example.com");
		invitation.setPlanTier(PlanTier.BASICO);
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setPaymentExempt(true);
		invitation.setCreatedAt(Instant.parse("2026-01-01T12:00:00Z"));
		invitation.setExpiresAt(Instant.parse("2026-01-08T12:00:00Z"));
		when(gridService.findPage(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(invitation)));
		when(gridService.countFiltered(any())).thenReturn(1L);
		when(gridService.countAll()).thenReturn(10L);
		final PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setDraw(1);
		pagingRequest.setStart(0);
		pagingRequest.setLength(25);

		final PageArray pageArray = controller.getPageArray(pagingRequest);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "invitations.list");
		assertThat(pageArray.getRecordsTotal()).isEqualTo(10);
		assertThat(pageArray.getRecordsFiltered()).isEqualTo(1);
		assertThat(pageArray.getData()).hasSize(1);
		assertThat(pageArray.getData().get(0).get(1)).isEqualTo("nutri@example.com");
	}

}
