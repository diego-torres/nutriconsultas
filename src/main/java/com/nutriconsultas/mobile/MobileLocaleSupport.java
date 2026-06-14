package com.nutriconsultas.mobile;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Locale resolution for {@code /rest/mobile/**} requests (#111).
 */
public final class MobileLocaleSupport {

	private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es-MX");

	private MobileLocaleSupport() {
	}

	public static Locale resolve(final HttpServletRequest request) {
		if (request == null) {
			final Locale contextLocale = LocaleContextHolder.getLocale();
			return contextLocale != null ? contextLocale : DEFAULT_LOCALE;
		}
		final String acceptLanguage = request.getHeader("Accept-Language");
		if (!StringUtils.hasText(acceptLanguage)) {
			return DEFAULT_LOCALE;
		}
		final String firstTag = acceptLanguage.split(",")[0].trim();
		if (!StringUtils.hasText(firstTag)) {
			return DEFAULT_LOCALE;
		}
		return Locale.forLanguageTag(firstTag.replace('_', '-'));
	}

}
