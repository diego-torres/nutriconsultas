package com.nutriconsultas.auth0;

public record Auth0PatientTokenResponse(String accessToken, String idToken, String refreshToken, long expiresIn,
		String tokenType) {

}
