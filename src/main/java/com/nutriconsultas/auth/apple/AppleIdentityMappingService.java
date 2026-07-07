package com.nutriconsultas.auth.apple;

/**
 * Maps Apple Sign-In notification subjects to Auth0 users and local patient accounts
 * (#504).
 */
@FunctionalInterface
public interface AppleIdentityMappingService {

	AppleIdentityMappingResult mapNotification(String appleSubject, String email);

}
