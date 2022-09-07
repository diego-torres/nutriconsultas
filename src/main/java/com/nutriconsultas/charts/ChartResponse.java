package com.nutriconsultas.charts;

import java.util.List;
import java.util.Map;

public class ChartResponse {
  private List<String> labels;
  private Map<String, Object> data;

  public List<String> getLabels() {
    return labels;
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

}
