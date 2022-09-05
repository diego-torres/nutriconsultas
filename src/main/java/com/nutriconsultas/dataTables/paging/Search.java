package com.nutriconsultas.dataTables.paging;

public class Search {
  private String value;
  private String regexp;

  public Search() {
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getRegexp() {
    return regexp;
  }

  public void setRegexp(String regexp) {
    this.regexp = regexp;
  }
}
