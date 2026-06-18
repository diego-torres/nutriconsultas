package com.nutriconsultas.auth0;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.nutriconsultas.mobile.Auth0ManagementNotConfiguredException;
import com.nutriconsultas.subscription.PlanTier;

@Component
@ConditionalOnMissingBean(Auth0RoleSyncClientImpl.class)
public class NoOpAuth0RoleSyncClient implements Auth0RoleSyncClient {

	@Override
	public boolean isConfigured() {
		return false;
	}

	@Override
	public void syncPlanRole(final String auth0UserId, final PlanTier planTier) {
		throw new Auth0ManagementNotConfiguredException();
	}

	@Override
	public void revokePlanRoles(final String auth0UserId) {
		throw new Auth0ManagementNotConfiguredException();
	}

}
