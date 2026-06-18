package com.nutriconsultas.subscription.invitation;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.SubscriptionEntitlementService;

@ExtendWith(MockitoExtension.class)
class ClinicInvitationServiceTest {

	private static final String DIRECTOR_ID = "auth0|director-1";

	@InjectMocks
	private ClinicInvitationService clinicInvitationService;

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Test
	void assertCanInviteNutritionistDelegatesToEntitlementService() {
		clinicInvitationService.assertCanInviteNutritionist(DIRECTOR_ID);

		verify(subscriptionEntitlementService).assertCanInviteNutritionist(DIRECTOR_ID);
	}

}
