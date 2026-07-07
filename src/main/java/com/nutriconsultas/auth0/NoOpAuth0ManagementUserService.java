package com.nutriconsultas.auth0;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(Auth0ManagementUserServiceImpl.class)
public class NoOpAuth0ManagementUserService implements Auth0ManagementUserService {

	@Override
	public boolean isConfigured() {
		return false;
	}

	@Override
	public Optional<Auth0ManagementUser> findUserByAppleSubject(final String appleSubject) {
		return Optional.empty();
	}

	@Override
	public List<Auth0ManagementUser> searchUsersByEmail(final String email) {
		return List.of();
	}

	@Override
	public void updateAppMetadata(final String auth0UserId, final Map<String, Object> appMetadataPatch) {
		throw new IllegalStateException("Auth0 Management API is not configured");
	}

	@Override
	public void blockUserInAppMetadata(final String auth0UserId) {
		throw new IllegalStateException("Auth0 Management API is not configured");
	}

	@Override
	public void deleteUser(final String auth0UserId) {
		throw new IllegalStateException("Auth0 Management API is not configured");
	}

}
