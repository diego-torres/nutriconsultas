package com.nutriconsultas.platillos;

import java.util.List;

import org.springframework.lang.NonNull;

public interface PlatilloService {
    List<Platillo> findAll();
    Platillo findById(@NonNull Long id);
    Platillo save(@NonNull Platillo platillo);
}
