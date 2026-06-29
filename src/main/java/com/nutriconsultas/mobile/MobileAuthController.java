package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.booking.ClientIpResolver;
import com.nutriconsultas.mobile.auth.PatientMobileAuthService;
import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatientAuthTokensDto;
import com.nutriconsultas.mobile.dto.PatientLoginRequest;
import com.nutriconsultas.mobile.dto.PatientSignupRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/auth")
@Tag(name = "Mobile Auth", description = "Backend-brokered patient database signup and login")
@Slf4j
public class MobileAuthController {

	private final PatientMobileAuthService patientMobileAuthService;

	private final PatientAuthRateLimiter patientAuthRateLimiter;

	public MobileAuthController(final PatientMobileAuthService patientMobileAuthService,
			final PatientAuthRateLimiter patientAuthRateLimiter) {
		this.patientMobileAuthService = patientMobileAuthService;
		this.patientAuthRateLimiter = patientAuthRateLimiter;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register patient Auth0 account",
			description = "Public, rate-limited. Validates pending invitation, creates Auth0 database user server-side, returns OIDC tokens.")
	@SecurityRequirements
	@MobileOpenApiResponses.PublicInvitationPreview
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
			description = "Auth0 account created and session tokens issued")
	public ApiResponse<PatientAuthTokensDto> signUp(@Valid @RequestBody final PatientSignupRequest request,
			final HttpServletRequest httpRequest) {
		final String clientKey = ClientIpResolver.resolve(httpRequest);
		if (log.isDebugEnabled()) {
			log.debug("Mobile patient signup broker request from clientKey={}", clientKey);
		}
		final PatientAuthTokensDto tokens = patientAuthRateLimiter.execute(clientKey, () -> patientMobileAuthService
			.signUp(request.email(), request.password(), request.displayName(), request.token(), request.humanCode()));
		return ApiResponse.ok(tokens);
	}

	@PostMapping("/login")
	@Operation(summary = "Patient database login",
			description = "Public, rate-limited. Authenticates via Auth0 server-side broker and returns OIDC tokens.")
	@SecurityRequirements
	@MobileOpenApiResponses.PublicInvitationPreview
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session tokens issued")
	public ApiResponse<PatientAuthTokensDto> login(@Valid @RequestBody final PatientLoginRequest request,
			final HttpServletRequest httpRequest) {
		final String clientKey = ClientIpResolver.resolve(httpRequest);
		if (log.isDebugEnabled()) {
			log.debug("Mobile patient login broker request from clientKey={}", clientKey);
		}
		final PatientAuthTokensDto tokens = patientAuthRateLimiter.execute(clientKey, () -> patientMobileAuthService
			.login(request.email(), request.password(), request.token(), request.humanCode()));
		return ApiResponse.ok(tokens);
	}

}
