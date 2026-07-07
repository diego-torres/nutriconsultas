package com.nutriconsultas.auth.apple;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppleSignInRemoteJwksKeySource implements AppleSignInJwksKeySource {

	private final AppleSignInProperties properties;

	private volatile JWKSource<SecurityContext> cachedKeySource;

	public AppleSignInRemoteJwksKeySource(final AppleSignInProperties properties) {
		this.properties = properties;
	}

	@Override
	public JWKSource<SecurityContext> getKeySource() {
		JWKSource<SecurityContext> keySource = cachedKeySource;
		if (keySource == null) {
			synchronized (this) {
				keySource = cachedKeySource;
				if (keySource == null) {
					keySource = buildKeySource();
					cachedKeySource = keySource;
				}
			}
		}
		return keySource;
	}

	private JWKSource<SecurityContext> buildKeySource() {
		try {
			final URL jwksUrl = new URL(properties.getJwksUrl());
			return JWKSourceBuilder.create(jwksUrl).cache(properties.getJwksCacheTtl().toMillis(), 60_000L).build();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid Apple JWKS URL: " + properties.getJwksUrl(), ex);
		}
	}

}
