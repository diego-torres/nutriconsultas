package com.nutriconsultas.auth.apple;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Supplies Apple JWKS keys for notification signature verification (#501).
 */
public interface AppleSignInJwksKeySource {

	JWKSource<SecurityContext> getKeySource();

}
