package com.nutriconsultas.ai;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Test helper for AI components that require {@link AiAuditLogger} (#397, #398).
 */
final class AiMetricsTestSupport {

	private AiMetricsTestSupport() {
	}

	static AiAuditLogger auditLogger() {
		return new AiAuditLogger(new AiUsageMetrics(new SimpleMeterRegistry()));
	}

	static AiUsageMetrics usageMetrics() {
		return new AiUsageMetrics(new SimpleMeterRegistry());
	}

}
