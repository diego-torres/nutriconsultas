package com.nutriconsultas.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AdminMultipartExceptionHandlerTest {

	@Test
	void handleMaxUploadSizeExceededRedirectsToPlatilloFormWithSpanishMessage() {
		final AdminMultipartExceptionHandler handler = new AdminMultipartExceptionHandler(DataSize.ofMegabytes(10));
		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/platillos/42/picture");
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String view = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(1024L), request,
				redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/platillos/42");
		assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
			.isEqualTo("La imagen supera el tamaño máximo permitido (10 MB).");
	}

	@Test
	void handleMaxUploadSizeExceededRedirectsToProfileFormWithSpanishMessage() {
		final AdminMultipartExceptionHandler handler = new AdminMultipartExceptionHandler(DataSize.ofMegabytes(10));
		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/perfil/logo");
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String view = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(1024L), request,
				redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/perfil");
		assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
			.isEqualTo("El archivo supera el tamaño máximo permitido (10 MB).");
	}

}
