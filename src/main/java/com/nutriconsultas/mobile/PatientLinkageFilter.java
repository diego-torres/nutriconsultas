package com.nutriconsultas.mobile;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nutriconsultas.mobile.MobilePatientAccessRules.Decision;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PatientLinkageFilter extends OncePerRequestFilter {

	private final CurrentPatientService currentPatientService;

	private final MobileApiErrorResponses errorResponses;

	public PatientLinkageFilter(final CurrentPatientService currentPatientService,
			final MobileApiErrorResponses errorResponses) {
		this.currentPatientService = currentPatientService;
		this.errorResponses = errorResponses;
	}

	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) {
		final String path = request.getRequestURI();
		return path == null || !path.startsWith(MobilePatientAccessRules.PATIENT_API_PREFIX);
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain filterChain) throws ServletException, IOException {
		if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
			filterChain.doFilter(request, response);
			return;
		}

		final Jwt jwt = jwtAuth.getToken();
		final String path = request.getRequestURI();
		final Optional<PacienteAuthView> authView = currentPatientService.findAuthViewByJwt(jwt);
		final Decision decision = MobilePatientAccessRules.evaluatePatientApiAccess(authView, path);
		if (decision == Decision.ONBOARDING_REQUIRED) {
			if (log.isDebugEnabled()) {
				log.debug("Mobile patient API blocked by onboarding gate");
			}
			writeOnboardingRequired(request, response);
			return;
		}

		authView.map(CurrentPatient::from).map(CurrentPatient::toPrincipal).ifPresent(jwtAuth::setDetails);
		filterChain.doFilter(request, response);
	}

	private void writeOnboardingRequired(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		errorResponses.writeJson(response, HttpServletResponse.SC_FORBIDDEN,
				errorResponses.error(MobileApiErrorResponses.KEY_PATIENT_ONBOARDING_REQUIRED, request));
	}

}
