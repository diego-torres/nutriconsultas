package com.nutriconsultas.dataTables.paging;

import java.util.List;

public class PagingRequest {
  private int start;
  private int length;
  private int draw;
  private List<Order> order;
  private List<Column> columns;
  private Search search;

  public PagingRequest() {
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public int getDraw() {
    return draw;
  }

  public void setDraw(int draw) {
    this.draw = draw;
  }

  public List<Order> getOrder() {
    return order;
  }

  public void setWorkOrder(List<Order> order) {
    this.order = order;
  }

  public List<Column> getColumns() {
    return columns;
  }

  public void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  public Search getSearch() {
    return search;
  }

  public void setSearch(Search search) {
    this.search = search;
  }
}
