package com.nutriconsultas.booking;

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
 * Per-nutritionist booking preferences: slot length and timezone (#246).
 */
@Entity
@Table(name = "nutritionist_availability_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionistAvailabilitySettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true, length = 255)
	private String userId;

	@Column(name = "slot_duration_minutes", nullable = false)
	private Integer slotDurationMinutes = BookingAvailabilityConstants.DEFAULT_SLOT_DURATION_MINUTES;

	@Column(nullable = false, length = 64)
	private String timezone = BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID;

}
