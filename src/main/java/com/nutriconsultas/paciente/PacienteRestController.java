package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

@RestController
@RequestMapping("/rest")
public class PacienteRestController {
  private static final Logger logger = LoggerFactory.getLogger(PacienteRestController.class);
  private static final Comparator<Paciente> EMPTY_COMPARATOR = (o1, o2) -> 0;

  @Autowired
  private PacienteRepository repo;

  @PostMapping("pacientes")
  public PageArray array(@RequestBody PagingRequest pagingRequest) {
    pagingRequest.setColumns(
        Stream.of("name", "dob", "email", "phone", "gender", "responsible", "actions")
            .map(Column::new)
            .collect(Collectors.toList()));
    Page<Paciente> page = getRows(pagingRequest);
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

  private List<String> toStringList(Paciente row) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return Arrays.asList(row.getName(), //
        row.getDob() != null ? dateFormat.format(row.getDob()) : "", //
        row.getEmail(), //
        row.getPhone(), //
        row.getGender(), //
        row.getResponsibleName(), //
        "<a href='/admin/pacientes/" + row.getId() + "' class='btn btn-outline-dark btn-sm'>Detalle</a>");
  }

  private Page<Paciente> getRows(PagingRequest pagingRequest) {
    return getPage(StreamSupport
        .stream(repo.findAll().spliterator(), false)
        .collect(Collectors.toList()), pagingRequest);
  }

  private Page<Paciente> getPage(List<Paciente> rows, PagingRequest pagingRequest) {
    List<Paciente> filtered = rows.stream()
        .sorted(sortRows(pagingRequest))
        .filter(filterRows(pagingRequest))
        .skip(pagingRequest.getStart())
        .limit(pagingRequest.getLength())
        .collect(Collectors.toList());

    long count = rows.stream()
        .filter(filterRows(pagingRequest))
        .count();

    Page<Paciente> page = new Page<>(filtered);
    page.setRecordsFiltered((int) count);
    page.setRecordsTotal((int) count);
    page.setDraw(pagingRequest.getDraw());

    return page;
  }

  private Predicate<Paciente> filterRows(PagingRequest pagingRequest) {
    if (pagingRequest.getSearch() == null || !StringUtils.hasLength(pagingRequest.getSearch().getValue())) {
      return row -> true;
    }

    String value = pagingRequest.getSearch().getValue().toLowerCase();

    return row -> row.getName().toLowerCase().contains(value)
        || row.getName().toLowerCase().startsWith(value)
        || row.getResponsibleName().toLowerCase().contains(value)
        || row.getResponsibleName().toLowerCase().startsWith(value);
  }

  private Comparator<Paciente> sortRows(PagingRequest pagingRequest) {
    if (pagingRequest.getWorkOrder() == null) {
      return EMPTY_COMPARATOR;
    }

    try {
      Order workOrder = pagingRequest.getWorkOrder().get(0);

      int columnIndex = workOrder.getColumn();
      Column column = pagingRequest.getColumns().get(columnIndex);

      Comparator<Paciente> comparator = PacienteComparators.getComparator(column.getData(),
          workOrder.getDir());
      if (comparator == null) {
        return EMPTY_COMPARATOR;
      }

      return comparator;

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }

    return EMPTY_COMPARATOR;
  }
}
