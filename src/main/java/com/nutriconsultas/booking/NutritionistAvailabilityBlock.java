package com.nutriconsultas.booking;

import java.time.LocalDateTime;

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
 * Nutritionist absence or day-off window that removes slots from public booking (#247).
 */
@Entity
@Table(name = "nutritionist_availability_block")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionistAvailabilityBlock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "all_day", nullable = false)
	private boolean allDay;

	@Column(name = "start_date_time", nullable = false)
	private LocalDateTime startDateTime;

	@Column(name = "end_date_time", nullable = false)
	private LocalDateTime endDateTime;

}
