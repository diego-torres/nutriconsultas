package com.nutriconsultas.auth0;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Narrow Auth0 Management API operations for Apple Sign-In lifecycle handling (#505).
 */
public interface Auth0ManagementUserService {

	boolean isConfigured();

	Optional<Auth0ManagementUser> findUserByAppleSubject(String appleSubject);

	List<Auth0ManagementUser> searchUsersByEmail(String email);

	void updateAppMetadata(String auth0UserId, Map<String, Object> appMetadataPatch);

	void blockUserInAppMetadata(String auth0UserId);

	void deleteUser(String auth0UserId);

}
