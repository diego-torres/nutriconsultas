package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Accepts AI drafts and materializes them into catalog records (#382).
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AiDraftAcceptanceService {

	AiDraftAcceptanceResult accept(@NonNull Long draftId, @NonNull String nutritionistId, @NonNull OidcUser principal);

}
