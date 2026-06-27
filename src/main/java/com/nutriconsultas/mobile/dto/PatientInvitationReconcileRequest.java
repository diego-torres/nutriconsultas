package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional invitation credential for post-login reconciliation when secure storage no
 * longer holds the invite reference.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Optional invitation credential for reconcile")
public record PatientInvitationReconcileRequest(@Schema(description = "Raw invite URL token") String token,
		@Schema(description = "Human-readable invitation code") String humanCode) {

}
