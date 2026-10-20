package com.nutriconsultas.mobile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.AccountDeletionResultDto;
import com.nutriconsultas.mobile.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Account closure for any authenticated mobile user, including one who has not redeemed
 * an invitation yet. Lives outside {@code /rest/mobile/patient/**} so the onboarding gate
 * does not block it.
 */
@RestController
@RequestMapping("/rest/mobile/account")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobileAccountController {

	private final MobileAccountDeletionService mobileAccountDeletionService;

	public MobileAccountController(final MobileAccountDeletionService mobileAccountDeletionService) {
		this.mobileAccountDeletionService = mobileAccountDeletionService;
	}

	@DeleteMapping
	@Operation(summary = "Delete the signed-in account",
			description = "Permanently deletes the Auth0 account and, when linked, the patient record and in-app history.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deleted")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
			description = "Account deletion could not be completed")
	public ApiResponse<AccountDeletionResultDto> deleteAccount(@AuthenticationPrincipal final Jwt jwt) {
		if (log.isInfoEnabled()) {
			log.info("Mobile account deletion requested");
		}
		mobileAccountDeletionService.deleteAuthenticatedAccount(jwt);
		return ApiResponse.ok(AccountDeletionResultDto.deleted());
	}

}
