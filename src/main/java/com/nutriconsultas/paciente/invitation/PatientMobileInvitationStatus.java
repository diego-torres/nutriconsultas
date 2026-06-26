package com.nutriconsultas.paciente.invitation;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Web-safe mobile invitation state for nutritionist UI (#341).
 */
public record PatientMobileInvitationStatus(String stateCode, String stateLabel, boolean canSend, boolean canResend,
		boolean canRevoke, Long pendingInvitationId, String humanCode, Instant expiresAt,
		String recipientEmailRedacted) {

	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("stateCode", stateCode);
		map.put("stateLabel", stateLabel);
		map.put("canSend", canSend);
		map.put("canResend", canResend);
		map.put("canRevoke", canRevoke);
		if (pendingInvitationId != null) {
			map.put("pendingInvitationId", pendingInvitationId);
		}
		if (humanCode != null) {
			map.put("humanCode", humanCode);
		}
		if (expiresAt != null) {
			map.put("expiresAt", expiresAt.toString());
		}
		if (recipientEmailRedacted != null) {
			map.put("recipientEmailRedacted", recipientEmailRedacted);
		}
		return map;
	}

}
