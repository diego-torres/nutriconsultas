package com.nutriconsultas.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistAvailabilityBlockServiceImpl implements NutritionistAvailabilityBlockService {

	private final NutritionistAvailabilityBlockRepository blockRepository;

	public NutritionistAvailabilityBlockServiceImpl(final NutritionistAvailabilityBlockRepository blockRepository) {
		this.blockRepository = blockRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<NutritionistAvailabilityBlock> findBlocksInRange(@NonNull final String userId,
			@NonNull final LocalDateTime rangeStart, @NonNull final LocalDateTime rangeEnd) {
		return blockRepository.findOverlappingRange(userId, rangeStart, rangeEnd);
	}

	@Override
	@Transactional
	public AvailabilityBlockDto createBlock(@NonNull final String userId, @NonNull final AvailabilityBlockDto block) {
		AvailabilityBlockValidator.validate(block);
		final NutritionistAvailabilityBlock entity = new NutritionistAvailabilityBlock();
		entity.setUserId(userId);
		entity.setTitle(block.getTitle().trim());
		entity.setAllDay(block.isAllDay());
		entity.setStartDateTime(block.getStartDateTime());
		entity.setEndDateTime(block.getEndDateTime());
		final NutritionistAvailabilityBlock saved = blockRepository.save(entity);
		if (log.isInfoEnabled()) {
			log.info("Created availability block id={} for user {}", saved.getId(), LogRedaction.redactUserId(userId));
		}
		return toDto(saved);
	}

	@Override
	@Transactional
	public void deleteBlock(@NonNull final String userId, @NonNull final Long blockId) {
		final NutritionistAvailabilityBlock block = blockRepository.findByIdAndUserId(blockId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Bloqueo no encontrado"));
		blockRepository.delete(block);
		if (log.isInfoEnabled()) {
			log.info("Deleted availability block id={} for user {}", blockId, LogRedaction.redactUserId(userId));
		}
	}

	static AvailabilityBlockDto toDto(final NutritionistAvailabilityBlock block) {
		final AvailabilityBlockDto dto = new AvailabilityBlockDto();
		dto.setId(block.getId());
		dto.setTitle(block.getTitle());
		dto.setAllDay(block.isAllDay());
		dto.setStartDateTime(block.getStartDateTime());
		dto.setEndDateTime(block.getEndDateTime());
		return dto;
	}

}
