package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.contact.ContactInquiryService;
import com.nutriconsultas.platform.PlatformAdminService;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class ContactInquiryAdminControllerTest {

	@InjectMocks
	private ContactInquiryAdminController controller;

	@Mock
	private ContactInquiryService contactInquiryService;

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private OidcUser principal;

	@Test
	void list_whenNotPlatformAdmin_throwsForbidden() {
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(false);

		assertThatThrownBy(() -> controller.list(principal, new ExtendedModelMap()))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

}
