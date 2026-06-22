package com.nutriconsultas.subscription.clinic;

import java.util.List;

/**
 * Director patient transfer form state: active members and optional source roster.
 */
public record ClinicPatientTransferPage(List<ClinicMemberView> activeMembers, Long selectedSourceMemberId,
		List<ClinicPatientSummary> sourcePatients) {
}
