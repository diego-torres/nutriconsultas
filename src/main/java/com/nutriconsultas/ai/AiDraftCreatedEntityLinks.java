package com.nutriconsultas.ai;

/**
 * Admin catalog paths for entities created from accepted AI drafts.
 */
public final class AiDraftCreatedEntityLinks {

	private AiDraftCreatedEntityLinks() {
	}

	public static String path(final AiDraftCreatedEntityType entityType, final long entityId) {
		return switch (entityType) {
			case PLATILLO -> "/admin/platillos/" + entityId;
			case DIETA -> "/admin/dietas/" + entityId;
		};
	}

}
