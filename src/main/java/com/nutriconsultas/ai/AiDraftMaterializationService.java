package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.platillos.Platillo;

/**
 * Converts accepted AI draft payloads into catalog entities (#382).
 */
public interface AiDraftMaterializationService {

	Platillo materializeDish(@NonNull DishDraftPayload payload, @NonNull String nutritionistId,
			@NonNull OidcUser principal);

	Dieta materializeMenu(@NonNull MenuDraftPayload payload, @NonNull String nutritionistId,
			@NonNull OidcUser principal);

	Dieta materializeDietPlan(@NonNull DietPlanDraftPayload payload, @NonNull String nutritionistId,
			@NonNull OidcUser principal);

}
