package com.nutriconsultas.dieta;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.platillos.Platillo;

public interface PlatilloFromIngestaService {

	Platillo createFromIngestaSelection(@NonNull Ingesta ingesta, @NonNull CreatePlatilloFromIngestaRequest request,
			@NonNull String userId, OidcUser principal);

	Dieta replaceSelectionWithCatalogPlatillo(@NonNull Dieta dieta, @NonNull Ingesta ingesta,
			@NonNull Long catalogPlatilloId, @NonNull ReplaceIngestaSelectionRequest request);

}
