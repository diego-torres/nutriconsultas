package com.nutriconsultas.mobile;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PatientLinkageFilter extends OncePerRequestFilter {

	private static final String PATIENT_API_PREFIX = "/rest/mobile/patient/";

	private final PatientAuthService patientAuthService;

	private final MobileApiErrorResponses errorResponses;

	public PatientLinkageFilter(final PatientAuthService patientAuthService,
			final MobileApiErrorResponses errorResponses) {
		this.patientAuthService = patientAuthService;
		this.errorResponses = errorResponses;
	}

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		final String path = request.getRequestURI();
		return path == null || !path.startsWith(PATIENT_API_PREFIX);
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain filterChain) throws ServletException, IOException {
		if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
			filterChain.doFilter(request, response);
			return;
		}

		final Jwt jwt = jwtAuth.getToken();
		try {
			final PatientPrincipal principal = patientAuthService.resolvePrincipal(jwt);
			jwtAuth.setDetails(principal);
			filterChain.doFilter(request, response);
		}
		catch (PatientNotLinkedException ex) {
			if (log.isDebugEnabled()) {
				log.debug("Mobile JWT sub has no linked Paciente.patientAuthSub");
			}
			writeForbidden(request, response);
		}
	}

	private void writeForbidden(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		errorResponses.writeJson(response, HttpServletResponse.SC_FORBIDDEN,
				errorResponses.error(MobileApiErrorResponses.KEY_PATIENT_NOT_LINKED, request));
	}

}
