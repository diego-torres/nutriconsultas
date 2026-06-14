package com.nutriconsultas.mobile.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LocaleContextFilterTest {

	private final LocaleContextFilter filter = new LocaleContextFilter();

	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void setsDefaultLocaleWhenHeaderAbsent() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/rest/mobile/patient/visits");
		final MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response,
				(req, res) -> assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.forLanguageTag("es-MX")));
	}

	@Test
	void setsLocaleFromAcceptLanguageHeader() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/rest/mobile/patient/visits");
		request.addHeader("Accept-Language", "en");
		final MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response,
				(req, res) -> assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.ENGLISH));
	}

	@Test
	void skipsNonMobilePaths() throws Exception {
		LocaleContextHolder.setLocale(Locale.GERMAN);
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/pacientes");
		final MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response,
				(req, res) -> assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN));
	}

}
