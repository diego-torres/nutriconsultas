package com.nutriconsultas.mobile.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class MobileDtoSerializationTest {

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Test
	void apiResponse_ok_omitsNullMessageAndUsesIso8601Timestamp() throws Exception {
		final ApiResponse<String> response = ApiResponse.ok("payload");
		final JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(json.get("data").asText()).isEqualTo("payload");
		assertThat(json.has("message")).isFalse();
		assertThat(json.get("timestamp").asText()).matches("\\d{4}-\\d{2}-\\d{2}T.*");
	}

	@Test
	void pagedResponse_of_mapsSpringPage() throws Exception {
		final Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);
		final PagedResponse<String> response = PagedResponse.of(page);
		final JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(json.get("content")).hasSize(2);
		assertThat(json.get("page").asInt()).isEqualTo(1);
		assertThat(json.get("size").asInt()).isEqualTo(2);
		assertThat(json.get("totalElements").asLong()).isEqualTo(5);
		assertThat(json.get("totalPages").asInt()).isEqualTo(3);
		assertThat(json.get("last").asBoolean()).isFalse();
	}

	@Test
	void cursorPagedResponse_of_omitsBlankCursor() throws Exception {
		final CursorPagedResponse<Instant> lastPage = CursorPagedResponse
			.of(List.of(Instant.parse("2026-01-01T00:00:00Z")), null);
		final JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(lastPage));

		assertThat(json.get("hasMore").asBoolean()).isFalse();
		assertThat(json.has("nextCursor")).isFalse();
	}

	@Test
	void cursorPagedResponse_of_includesNextCursorWhenPresent() throws Exception {
		final CursorPagedResponse<LocalDateTime> page = CursorPagedResponse.of(List.of(), "cursor-abc");
		final JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(page));

		assertThat(json.get("nextCursor").asText()).isEqualTo("cursor-abc");
		assertThat(json.get("hasMore").asBoolean()).isTrue();
	}

}
