package com.nutriconsultas.subscription.invitation;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Platform-admin paid onboarding invitations for nutritionists and clinic directors.
 */
public interface NutritionistInvitationService {

	CreatedNutritionistInvitation createInvitation(OidcUser adminPrincipal, String email, PlanTier planTier,
			boolean paymentExempt);

	RedeemNutritionistInvitationResult redeemInvitation(OidcUser principal, String rawToken);

}
