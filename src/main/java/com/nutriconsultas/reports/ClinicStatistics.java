package com.nutriconsultas.reports;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for clinic statistics report.
 *
 * <p>
 * Contains aggregated statistics across all patients for a specific user, including
 * demographics, consultation trends, conditions, and performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicStatistics {

	// Summary metrics
	private Long totalPatients;

	private Long totalConsultations;

	private Long totalDietaryPlans;

	private Long totalClinicalExams;

	private Long totalAnthropometricMeasurements;

	// Demographics
	private Map<String, Long> genderDistribution;

	private Map<String, Long> ageGroupDistribution;

	private Map<String, Long> weightLevelDistribution;

	// Consultation trends
	private Map<String, Long> consultationsByMonth;

	private Map<String, Long> consultationsByStatus;

	private Double averageConsultationsPerPatient;

	// Most common conditions
	private Map<String, Long> conditionFrequency;

	// Weight/BMI metrics
	private Double averageWeight;

	private Double averageBMI;

	private Double averageWeightChange;

	private Double averageBMIChange;

	// Date range
	private Date startDate;

	private Date endDate;

	private Date reportDate;

	// Additional metrics
	private Long activeDietaryPlans;

	private Long completedConsultations;

	private Long newPatientsInPeriod;

	// Trend data for charts
	private List<MonthlyTrend> monthlyTrends;

	/**
	 * Data point for monthly trend visualization.
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MonthlyTrend {

		private String month;

		private Long consultations;

		private Long newPatients;

		private Long activePlans;

	}

}
