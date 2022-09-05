package com.nutriconsultas.dataTables.paging;

import java.util.List;

public class Page<T> {
  private List<T> data;
  private int recordsFiltered;
  private int recordsTotal;
  private int draw;

  public Page() {
  }

  public Page(List<T> data) {
    this.data = data;
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
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
