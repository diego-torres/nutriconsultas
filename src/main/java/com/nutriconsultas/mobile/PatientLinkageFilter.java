package com.nutriconsultas.mobile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
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

	public PatientLinkageFilter(final PatientAuthService patientAuthService) {
		this.patientAuthService = patientAuthService;
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
			writeForbidden(response);
		}
	}

	private void writeForbidden(final HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"patient_not_linked\"}");
	}

}
