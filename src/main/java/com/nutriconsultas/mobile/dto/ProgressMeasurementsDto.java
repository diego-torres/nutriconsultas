package com.nutriconsultas.mobile.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Time-series payload for {@code GET /rest/mobile/patient/progress/measurements} (#99).
 * Points are ordered ascending by {@code recordedAt} for charting.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProgressMeasurementsDto(List<ProgressMeasurementPointDto> measurements, int count, boolean truncated) {
}
