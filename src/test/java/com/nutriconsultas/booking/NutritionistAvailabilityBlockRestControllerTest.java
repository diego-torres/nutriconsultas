package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class NutritionistAvailabilityBlockRestControllerTest {

	@InjectMocks
	private NutritionistAvailabilityBlockRestController controller;

	@Mock
	private NutritionistAvailabilityBlockService blockService;

	@Mock
	private OidcUser principal;

	@Test
	void listBlocksRequiresRangeAndAuth() {
		assertThat(controller.listBlocks(new Date(), new Date(), null)).isEmpty();
	}

	@Test
	void createBlockReturnsCalendarPayload() {
		when(principal.getSubject()).thenReturn("auth0|user1");
		final AvailabilityBlockDto dto = new AvailabilityBlockDto();
		dto.setTitle("Conferencia");
		dto.setAllDay(false);
		dto.setStartDateTime(LocalDateTime.of(2026, 6, 21, 13, 0));
		dto.setEndDateTime(LocalDateTime.of(2026, 6, 21, 17, 0));
		dto.setId(3L);
		when(blockService.createBlock("auth0|user1", dto)).thenReturn(dto);

		final ResponseEntity<Map<String, Object>> response = controller.createBlock(dto,
				new org.springframework.validation.BeanPropertyBindingResult(dto, "block"), principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true);
	}

	@Test
	void deleteBlockReturnsSuccess() {
		when(principal.getSubject()).thenReturn("auth0|user1");

		final ResponseEntity<Map<String, Object>> response = controller.deleteBlock(8L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(blockService).deleteBlock("auth0|user1", 8L);
	}

}
