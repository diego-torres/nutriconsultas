package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.auth0.Auth0PatientTokenResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Auth0 OIDC tokens brokered for the mobile app")
public record PatientAuthTokensDto(@Schema(description = "Access token for /rest/mobile/**") String accessToken,
		@Schema(description = "OpenID Connect ID token") String idToken,
		@Schema(description = "Refresh token when offline_access is granted") String refreshToken,
		@Schema(description = "Access token lifetime in seconds") long expiresIn,
		@Schema(description = "Token type, typically Bearer") String tokenType) {

	public static PatientAuthTokensDto from(final Auth0PatientTokenResponse tokens) {
		return new PatientAuthTokensDto(tokens.accessToken(), tokens.idToken(), tokens.refreshToken(),
				tokens.expiresIn(), tokens.tokenType());
	}

}
