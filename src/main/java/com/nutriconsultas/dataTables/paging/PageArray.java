package com.nutriconsultas.dataTables.paging;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageArray {
  private List<List<String>> data;
  private int recordsFiltered;
  private int recordsTotal;
  private int draw;
}
