package com.nutriconsultas.dieta;

import java.util.List;

import org.springframework.lang.NonNull;

public interface DietaService {
    Dieta getDieta(@NonNull Long id);
    void saveDieta(@NonNull Dieta dieta);
    void deleteDieta(@NonNull Long id);
    List<Dieta> getDietas();
}
