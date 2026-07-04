package com.nutriconsultas.ai;

/**
 * Estimated bulk units from the scope classifier (#448).
 */
public record AiRequestScopeRequestedUnits(Integer days, Integer dishes, Integer plans, Integer patients) {

	public static AiRequestScopeRequestedUnits empty() {
		return new AiRequestScopeRequestedUnits(null, null, null, null);
	}

}
