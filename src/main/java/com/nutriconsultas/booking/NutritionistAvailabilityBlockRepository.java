package com.nutriconsultas.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionistAvailabilityBlockRepository extends JpaRepository<NutritionistAvailabilityBlock, Long> {

	@Query("SELECT b FROM NutritionistAvailabilityBlock b WHERE b.userId = :userId "
			+ "AND b.endDateTime > :rangeStart AND b.startDateTime < :rangeEnd " + "ORDER BY b.startDateTime ASC")
	List<NutritionistAvailabilityBlock> findOverlappingRange(@Param("userId") String userId,
			@Param("rangeStart") LocalDateTime rangeStart, @Param("rangeEnd") LocalDateTime rangeEnd);

	Optional<NutritionistAvailabilityBlock> findByIdAndUserId(Long id, String userId);

}
