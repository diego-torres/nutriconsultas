package com.nutriconsultas.platillos;

import java.util.List;

import org.springframework.lang.NonNull;

public interface PlatilloService {
  List<Platillo> findAll();

  Platillo findById(@NonNull Long id);

  Platillo save(@NonNull Platillo platillo);

  void deleteIngrediente(@NonNull Long id, @NonNull Long ingredienteId);

  Ingrediente addIngrediente(@NonNull Long id, @NonNull Long alimentoId, @NonNull String cantidad,
      @NonNull Integer peso);
}
