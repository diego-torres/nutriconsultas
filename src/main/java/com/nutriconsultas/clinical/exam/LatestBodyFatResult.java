package com.nutriconsultas.clinical.exam;

import java.time.Instant;

public record LatestBodyFatResult(Double bodyFatPercentage, Instant measurementDate, BodyFatSource source) {
}
