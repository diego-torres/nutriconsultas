package com.nutriconsultas.platillos;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface PlatilloDeletionService {

	PlatilloDeleteResult deletePlatillo(@NonNull Long platilloId, @NonNull String userId, OidcUser principal);

	long countDietReferences(@NonNull Platillo platillo, @NonNull String userId);

}
