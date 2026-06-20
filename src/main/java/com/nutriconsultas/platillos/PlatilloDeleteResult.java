package com.nutriconsultas.platillos;

import lombok.Getter;

@Getter
public final class PlatilloDeleteResult {

	public enum Outcome {

		DELETED, NOT_FOUND, FORBIDDEN, IN_USE

	}

	private final Outcome outcome;

	private final long dietReferenceCount;

	private PlatilloDeleteResult(final Outcome outcome, final long dietReferenceCount) {
		this.outcome = outcome;
		this.dietReferenceCount = dietReferenceCount;
	}

	public static PlatilloDeleteResult deleted() {
		return new PlatilloDeleteResult(Outcome.DELETED, 0L);
	}

	public static PlatilloDeleteResult notFound() {
		return new PlatilloDeleteResult(Outcome.NOT_FOUND, 0L);
	}

	public static PlatilloDeleteResult forbidden() {
		return new PlatilloDeleteResult(Outcome.FORBIDDEN, 0L);
	}

	public static PlatilloDeleteResult inUse(final long dietReferenceCount) {
		return new PlatilloDeleteResult(Outcome.IN_USE, dietReferenceCount);
	}

}
