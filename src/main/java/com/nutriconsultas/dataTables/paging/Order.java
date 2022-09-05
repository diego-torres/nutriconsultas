package com.nutriconsultas.dataTables.paging;

public class Order {
  private Integer column;
  private Direction dir;

  public Order() {
  }

  public Order(Integer column, Direction dir) {
    this.column = column;
    this.dir = dir;
  }

  public Integer getColumn() {
    return column;
  }

  public void setColumn(Integer column) {
    this.column = column;
  }

  public Direction getDir() {
    return dir;
  }

  public void setDir(Direction dir) {
    this.dir = dir;
  }
}
