package com.nutriconsultas.mobile.filter;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sets {@link LocaleContextHolder} from {@code Accept-Language} for mobile API requests
 * (#111).
 */
@Component
public class LocaleContextFilter extends OncePerRequestFilter {

	private static final String MOBILE_API_PREFIX = "/rest/mobile/";

	private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es-MX");

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		final String path = request.getRequestURI();
		return path == null || !path.startsWith(MOBILE_API_PREFIX);
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain filterChain) throws ServletException, IOException {
		final Locale locale = resolveLocale(request.getHeader("Accept-Language"));
		LocaleContextHolder.setLocale(locale);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	private Locale resolveLocale(final String acceptLanguage) {
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
