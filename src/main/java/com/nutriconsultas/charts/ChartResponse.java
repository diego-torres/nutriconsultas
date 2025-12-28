package com.nutriconsultas.charts;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartResponse {

	private List<String> labels;

	private Map<String, Object> data;

}
