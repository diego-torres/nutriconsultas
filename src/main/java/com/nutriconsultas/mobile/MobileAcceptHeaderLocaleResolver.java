package com.nutriconsultas.mobile;

import java.util.Locale;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link LocaleResolver} for mobile API paths with {@code es-MX} default (#111).
 */
@Component
@Primary
public class MobileAcceptHeaderLocaleResolver extends AcceptHeaderLocaleResolver {

	@Override
	public Locale resolveLocale(final HttpServletRequest request) {
		if (!isMobileApiRequest(request)) {
			return super.resolveLocale(request);
		}
		final String acceptLanguage = request.getHeader("Accept-Language");
		if (!StringUtils.hasText(acceptLanguage)) {
			return Locale.forLanguageTag("es-MX");
		}
		return super.resolveLocale(request);
	}

	@Override
	public void setLocale(final HttpServletRequest request, final HttpServletResponse response, final Locale locale) {
		if (!isMobileApiRequest(request)) {
			super.setLocale(request, response, locale);
		}
	}

	private static boolean isMobileApiRequest(final HttpServletRequest request) {
		final String path = request.getRequestURI();
		return path != null && path.startsWith("/rest/mobile/");
	}

}
