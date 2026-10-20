package com.nutriconsultas.mobile.dto;

/**
 * Result of {@code DELETE /rest/mobile/account} (App Store Guideline 5.1.1(v)).
 *
 * @param accountDeleted always {@code true} when the response is HTTP 200
 */
public record AccountDeletionResultDto(boolean accountDeleted) {

	public static AccountDeletionResultDto deleted() {
		return new AccountDeletionResultDto(true);
	}

}
