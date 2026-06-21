package com.nutriconsultas.booking;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One contiguous working-hours window for a weekday (ISO-8601 day 1=Monday … 7=Sunday).
 */
@Entity
@Table(name = "nutritionist_working_hours_interval")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionistWorkingHoursInterval {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	/**
	 * {@link java.time.DayOfWeek#getValue()} — 1 (Monday) through 7 (Sunday).
	 */
	@Column(name = "day_of_week", nullable = false)
	private Integer dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

}
