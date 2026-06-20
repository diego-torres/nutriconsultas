package com.nutriconsultas.dieta;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface DietaDeletionService {

	DietaDeleteResult deleteDieta(@NonNull Long dietaId, @NonNull String userId, OidcUser principal);

	long countPatientAssignments(@NonNull Dieta dieta, @NonNull String userId);

}
