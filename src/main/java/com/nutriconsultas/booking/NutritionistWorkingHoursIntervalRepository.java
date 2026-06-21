package com.nutriconsultas.booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionistWorkingHoursIntervalRepository
		extends JpaRepository<NutritionistWorkingHoursInterval, Long> {

	List<NutritionistWorkingHoursInterval> findByUserIdOrderByDayOfWeekAscStartTimeAsc(String userId);

	void deleteByUserId(String userId);

}
