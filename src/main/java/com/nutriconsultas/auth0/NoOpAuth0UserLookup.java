package com.nutriconsultas.auth0;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(Auth0UserLookupImpl.class)
public class NoOpAuth0UserLookup implements Auth0UserLookup {

	@Override
	public boolean isConfigured() {
		return false;
	}

	@Override
	public Optional<String> findUserIdByEmail(final String email) {
		return Optional.empty();
	}

	@Override
	public Optional<String> findEmailByUserId(final String userId) {
		return Optional.empty();
	}

}
