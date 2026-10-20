package com.nutriconsultas.mobile;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Patient-requested account deletion could not be completed.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AccountDeletionUnavailableException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public AccountDeletionUnavailableException() {
		super("account_deletion_unavailable");
	}

	public AccountDeletionUnavailableException(final Throwable cause) {
		super("account_deletion_unavailable", cause);
	}

}
