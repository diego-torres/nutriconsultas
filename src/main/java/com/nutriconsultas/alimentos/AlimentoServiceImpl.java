package com.nutriconsultas.alimentos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AlimentoServiceImpl implements AlimentoService {

    @Autowired
    AlimentosRepository alimentosRepository;

    @Override
    public List<Alimento> findAll() {
        log.info("Retrieving all alimentos from database.");
        return alimentosRepository.findAll();
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
