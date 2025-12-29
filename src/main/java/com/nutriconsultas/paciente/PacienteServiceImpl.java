package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PacienteServiceImpl implements PacienteService {

	@Autowired
	private PacienteRepository repo;

	@Override
	public void delete(@NonNull final Long id) {
		log.info("deleting Paciente with id {}.", id);
		repo.deleteById(id);
		log.info("Paciente {} deleted successfully.", id);
	}

	@Override
	public Paciente findById(@NonNull final Long id) {
		log.info("finding Paciente with id {}.", id);
		final Paciente paciente = repo.findById(id).orElse(null);
		log.info("Paciente found {}.", paciente);
		return paciente;
	}

	@Override
	public Paciente save(@NonNull final Paciente paciente) {
		log.info("saving Paciente {}.", paciente);
		final Paciente saved = repo.save(paciente);
		log.info("Paciente saved {}.", saved);
		return saved;
	}

	@Override
	public List<Paciente> findAll() {
		log.info("getting all Paciente records.");
		return repo.findAll();
	}

}
