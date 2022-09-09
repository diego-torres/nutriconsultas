package com.nutriconsultas.dieta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

@RestController
@RequestMapping("/rest")
public class DietasRestController {

  @Autowired
  private DietaRepository repo;

  @PostMapping("dietas")
  public PageArray array(@RequestBody PagingRequest pagingRequest) {
    pagingRequest.setColumns(
        Stream.of("nombre", "dob", "email", "phone", "gender", "responsible")
            .map(Column::new)
            .collect(Collectors.toList()));
    Page<Dieta> page = getRows(pagingRequest);
    PageArray pageArray = new PageArray();
    pageArray.setRecordsFiltered(page.getRecordsFiltered());
    pageArray.setRecordsTotal(page.getRecordsTotal());
    pageArray.setDraw(page.getDraw());
    pageArray.setData(page.getData()
        .stream()
        .map(this::toStringList)
        .collect(Collectors.toList()));

    return pageArray;
  }

  private List<String> toStringList(Dieta row) {
    return Arrays.asList("");
  }
  private Page<Dieta> getRows(PagingRequest pagingRequest) {
    return getPage(StreamSupport
        .stream(repo.findAll().spliterator(), false)
        .collect(Collectors.toList()), pagingRequest);
  }

  private Page<Dieta> getPage(List<Dieta> rows, PagingRequest pagingRequest) {
    List<Dieta> filtered = rows.stream()
        .skip(pagingRequest.getStart())
        .limit(pagingRequest.getLength())
        .collect(Collectors.toList());

    Page<Dieta> page = new Page<>(filtered);
    page.setRecordsFiltered(0);
    page.setRecordsTotal(0);
    page.setDraw(pagingRequest.getDraw());

    return page;
  }
}
