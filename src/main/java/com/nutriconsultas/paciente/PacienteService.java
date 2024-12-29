package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.lang.NonNull;

public interface PacienteService {
    Paciente findById(@NonNull Long id);
    List<Paciente> findAll();
    Paciente save(@NonNull Paciente paciente);
    void delete(@NonNull Long id);
}
