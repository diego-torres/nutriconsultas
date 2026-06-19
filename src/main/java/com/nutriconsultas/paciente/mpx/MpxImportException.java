package com.nutriconsultas.paciente.mpx;

/**
 * User-facing MPX import failure (#222). Message is safe to show in the admin UI.
 */
public class MpxImportException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MpxImportException(final String message) {
		super(message);
	}

	public MpxImportException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
