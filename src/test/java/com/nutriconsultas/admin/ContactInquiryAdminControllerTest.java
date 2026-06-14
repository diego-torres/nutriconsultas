package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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

	@Test
	void markAsRead_whenNotPlatformAdmin_throwsForbidden() {
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(false);

		assertThatThrownBy(() -> controller.markAsRead(principal, 1L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

	@Test
	void markAsRead_whenPlatformAdmin_redirectsToList() {
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(true);

		final String view = controller.markAsRead(principal, 1L);

		verify(contactInquiryService).markAsRead(1L);
		assertThat(view).isEqualTo("redirect:/admin/contact-inquiries");
	}

	@Test
	void delete_whenNotPlatformAdmin_throwsForbidden() {
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(false);

		assertThatThrownBy(() -> controller.delete(principal, 1L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

	@Test
	void delete_whenPlatformAdmin_redirectsToList() {
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(true);

		final String view = controller.delete(principal, 1L);

		verify(contactInquiryService).deleteById(1L);
		assertThat(view).isEqualTo("redirect:/admin/contact-inquiries");
	}

}
