package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NutritionistAvailabilityBlockServiceTest {

	private static final String USER_ID = "auth0|nutritionist";

	@InjectMocks
	private NutritionistAvailabilityBlockServiceImpl service;

	@Mock
	private NutritionistAvailabilityBlockRepository blockRepository;

	@Test
	void createBlockPersistsEntity() {
		final AvailabilityBlockDto dto = new AvailabilityBlockDto();
		dto.setTitle("Vacaciones");
		dto.setAllDay(true);
		dto.setStartDateTime(LocalDateTime.of(2026, 7, 1, 0, 0));
		dto.setEndDateTime(LocalDateTime.of(2026, 7, 1, 0, 0));
		when(blockRepository.save(any(NutritionistAvailabilityBlock.class))).thenAnswer(invocation -> {
			final NutritionistAvailabilityBlock saved = invocation.getArgument(0);
			saved.setId(10L);
			return saved;
		});

		final AvailabilityBlockDto result = service.createBlock(USER_ID, dto);

		assertThat(result.getId()).isEqualTo(10L);
		assertThat(result.getTitle()).isEqualTo("Vacaciones");
		final ArgumentCaptor<NutritionistAvailabilityBlock> captor = ArgumentCaptor
			.forClass(NutritionistAvailabilityBlock.class);
		verify(blockRepository).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
	}

	@Test
	void deleteBlockRequiresOwnership() {
		when(blockRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteBlock(USER_ID, 5L)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void findBlocksInRangeDelegatesToRepository() {
		final LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
		final LocalDateTime end = LocalDateTime.of(2026, 6, 30, 0, 0);
		when(blockRepository.findOverlappingRange(USER_ID, start, end)).thenReturn(List.of());

		assertThat(service.findBlocksInRange(USER_ID, start, end)).isEmpty();
		verify(blockRepository).findOverlappingRange(eq(USER_ID), eq(start), eq(end));
	}

}
