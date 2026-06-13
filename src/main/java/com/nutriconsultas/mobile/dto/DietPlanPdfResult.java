package com.nutriconsultas.mobile.dto;

/**
 * Binary PDF payload for {@code GET /rest/mobile/patient/diet-plans/{assignmentId}/pdf}
 * (#95).
 */
public record DietPlanPdfResult(byte[] content, String filename) {

}
