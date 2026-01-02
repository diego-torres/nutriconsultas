package com.nutriconsultas.alimentos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AlimentoServiceImpl implements AlimentoService {

	@Autowired
	private AlimentosRepository alimentosRepository;

	@Override
	public List<Alimento> findAll() {
		log.info("Retrieving all alimentos from database.");
		return alimentosRepository.findAll();
	}

	@Override
	public Page<Alimento> findAll(@NonNull final Pageable pageable) {
		log.info("Retrieving alimentos with pagination: {}", pageable);
		return alimentosRepository.findAll(pageable);
	}

	@Override
	public Page<Alimento> findBySearchTerm(@NonNull final String searchTerm, @NonNull final Pageable pageable) {
		log.info("Searching alimentos with term: {} and pagination: {}", searchTerm, pageable);
		final String searchPattern = "%" + searchTerm + "%";
		return alimentosRepository.findBySearchTerm(searchPattern, pageable);
	}

	@Override
	public long count() {
		log.info("Counting all alimentos.");
		return alimentosRepository.count();
	}

	@Override
	public long countBySearchTerm(@NonNull final String searchTerm) {
		log.info("Counting alimentos with search term: {}", searchTerm);
		final String searchPattern = "%" + searchTerm + "%";
		return alimentosRepository.countBySearchTerm(searchPattern);
	}

	@Override
	public Alimento findById(@NonNull Long id) {
		log.info("Retrieving alimento with id: {}", id);
		return alimentosRepository.findById(id).orElse(null);
	}

	@Override
	public Alimento save(@NonNull Alimento alimento) {
		log.info("Saving alimento: {}", alimento);
		return alimentosRepository.save(alimento);
	}

}
