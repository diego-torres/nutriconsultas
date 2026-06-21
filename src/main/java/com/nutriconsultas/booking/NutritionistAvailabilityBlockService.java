package com.nutriconsultas.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.NonNull;

public interface NutritionistAvailabilityBlockService {

	List<NutritionistAvailabilityBlock> findBlocksInRange(@NonNull String userId, @NonNull LocalDateTime rangeStart,
			@NonNull LocalDateTime rangeEnd);

	AvailabilityBlockDto createBlock(@NonNull String userId, @NonNull AvailabilityBlockDto block);

	void deleteBlock(@NonNull String userId, @NonNull Long blockId);

}
