package com.nutriconsultas.profile;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Service interface for managing a nutritionist's professional profile.
 *
 * <p>
 * Provides lazy-create semantics: calling {@link #getOrCreateProfile(String)} always
 * returns a usable profile object, creating an empty one if none exists yet.
 */
public interface NutritionistProfileService {

	/**
	 * Retrieves the profile for the given user, creating an empty one if none exists.
	 * @param userId the Auth0 subject identifier
	 * @return the profile (never null)
	 */
	@NonNull
	NutritionistProfile getOrCreateProfile(@NonNull String userId);

	/**
	 * Persists a profile, enforcing tenant isolation.
	 * @param profile the profile data to save
	 * @param userId the Auth0 subject identifier of the caller
	 * @return the saved profile
	 */
	@NonNull
	NutritionistProfile saveProfile(@NonNull NutritionistProfile profile, @NonNull String userId);

	/**
	 * Uploads and stores the logo image for a given user in S3.
	 * @param userId the Auth0 subject identifier
	 * @param bytes the raw image bytes
	 * @param fileExtension the extension without dot (e.g. "png")
	 */
	void saveLogo(@NonNull String userId, @NonNull byte[] bytes, @NonNull String fileExtension);

	/**
	 * Retrieves the logo bytes for a given user from S3.
	 * @param userId the Auth0 subject identifier
	 * @return the raw image bytes, or null if no logo has been uploaded
	 */
	@Nullable
	byte[] getLogo(@NonNull String userId);

	/**
	 * Retrieves the logo as a Base64-encoded Data URI suitable for inline embedding in
	 * Flying Saucer PDF templates (e.g. {@code data:image/png;base64,...}).
	 * @param userId the Auth0 subject identifier
	 * @return a Data URI string, or null if no logo is stored
	 */
	@Nullable
	String getLogoAsBase64DataUri(@NonNull String userId);

}
