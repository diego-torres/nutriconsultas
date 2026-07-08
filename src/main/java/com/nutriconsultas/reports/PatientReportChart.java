package com.nutriconsultas.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Progress indicator chart for patient PDF reports.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientReportChart {

	private String title;

	private String unit;

	private boolean hasData;

	private String svgMarkup;

	private String emptyMessage;

}
