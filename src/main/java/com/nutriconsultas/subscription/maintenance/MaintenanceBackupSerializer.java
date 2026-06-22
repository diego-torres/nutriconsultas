package com.nutriconsultas.subscription.maintenance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public final class MaintenanceBackupSerializer {

	private static final int SCHEMA_VERSION = 1;

	private final ObjectMapper objectMapper;

	public MaintenanceBackupSerializer() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public byte[] toGzippedJson(final String runId, final Instant exportedAt,
			final List<NutritionistTenantSnapshot> tenants) {
		final Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("schemaVersion", SCHEMA_VERSION);
		payload.put("runId", runId);
		payload.put("exportedAt", exportedAt);
		payload.put("tenants", tenants);
		try {
			final byte[] json = objectMapper.writeValueAsBytes(payload);
			return gzip(json);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to serialize maintenance backup", ex);
		}
	}

	private static byte[] gzip(final byte[] input) throws IOException {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
			gzip.write(input);
		}
		return buffer.toByteArray();
	}

	public record NutritionistTenantSnapshot(String userId, Long subscriptionId, Instant revokedAt,
			List<Map<String, Object>> patients, List<Long> dietaIds, List<Long> platilloIds,
			Map<String, Object> profile, Long clinicId, int patientCount) {

		public static NutritionistTenantSnapshot empty(final String userId, final Long subscriptionId,
				final Instant revokedAt) {
			return new NutritionistTenantSnapshot(userId, subscriptionId, revokedAt, List.of(), List.of(), List.of(),
					Map.of(), null, 0);
		}

	}

	public static Map<String, Object> patientSummary(final Long id) {
		final Map<String, Object> summary = new HashMap<>();
		summary.put("id", id);
		return summary;
	}

	public static List<Map<String, Object>> patientSummaries(final List<Long> patientIds) {
		final List<Map<String, Object>> summaries = new ArrayList<>();
		for (final Long patientId : patientIds) {
			summaries.add(patientSummary(patientId));
		}
		return summaries;
	}

}
