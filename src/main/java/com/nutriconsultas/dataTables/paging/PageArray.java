package com.nutriconsultas.dataTables.paging;

import java.util.List;

public class PageArray {
  private List<List<String>> data;
  private int recordsFiltered;
  private int recordsTotal;
  private int draw;

  public PageArray() {
  }

  public List<List<String>> getData() {
    return data;
  }

  public void setData(List<List<String>> data) {
    this.data = data;
  }

  public int getRecordsFiltered() {
    return recordsFiltered;
  }

  public void setRecordsFiltered(int recordsFiltered) {
    this.recordsFiltered = recordsFiltered;
  }

  public int getRecordsTotal() {
    return recordsTotal;
  }

  public void setRecordsTotal(int recordsTotal) {
    this.recordsTotal = recordsTotal;
  }

  public int getDraw() {
    return draw;
  }

  public void setDraw(int draw) {
    this.draw = draw;
  }
}
