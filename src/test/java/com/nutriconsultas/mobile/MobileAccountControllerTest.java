package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.mobile.dto.AccountDeletionResultDto;
import com.nutriconsultas.mobile.dto.ApiResponse;

@ExtendWith(MockitoExtension.class)
class MobileAccountControllerTest {

	@InjectMocks
	private MobileAccountController controller;

	@Mock
	private MobileAccountDeletionService mobileAccountDeletionService;

	@Test
	void deleteAccount_returnsDeletedEnvelope() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("auth0|patient").build();

		final ApiResponse<AccountDeletionResultDto> response = controller.deleteAccount(jwt);

		assertThat(response.data().accountDeleted()).isTrue();
		verify(mobileAccountDeletionService).deleteAuthenticatedAccount(jwt);
	}

}
