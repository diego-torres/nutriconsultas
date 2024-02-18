package com.nutriconsultas.alimentos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
    
}
