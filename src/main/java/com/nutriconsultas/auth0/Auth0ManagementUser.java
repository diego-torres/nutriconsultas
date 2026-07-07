package com.nutriconsultas.auth0;

import java.util.List;
import java.util.Map;

/**
 * Minimal Auth0 Management API user view for Apple Sign-In identity mapping (#505).
 */
public record Auth0ManagementUser(String userId, String email, Map<String, Object> appMetadata,
		List<Map<String, Object>> identities) {

	public static Auth0ManagementUser fromApiMap(final Map<String, Object> user) {
		if (user == null) {
			return null;
		}
		final Object userId = user.get("user_id");
		final Object email = user.get("email");
		@SuppressWarnings("unchecked")
		final Map<String, Object> appMetadata = user.get("app_metadata") instanceof Map<?, ?> metadata
				? (Map<String, Object>) metadata : Map.of();
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> identities = user.get("identities") instanceof List<?> identityList
				? (List<Map<String, Object>>) identityList : List.of();
		return new Auth0ManagementUser(userId == null ? null : userId.toString(),
				email == null ? null : email.toString(), appMetadata, identities);
	}

}
