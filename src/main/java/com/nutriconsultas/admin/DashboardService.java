package com.nutriconsultas.admin;

import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;

public interface DashboardService {

	DashboardStatistics getDashboardStatistics(@NonNull String userId);

	List<Map<String, Object>> getPatientGrowthTrend(@NonNull String userId, int months);

	List<Map<String, Object>> getConsultationFrequency(@NonNull String userId, int months);

	List<Map<String, Object>> getMostCommonConditions(@NonNull String userId);

}
