package com.nutriconsultas.platillos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PlatilloServiceImpl implements PlatilloService {
    @Autowired
    PlatilloRepository platilloRepository;

    @Override
    public Platillo findById(@NonNull Long id) {
        log.info("Retrieving platillo with id: {}", id);
        return platilloRepository.findById(id).orElse(null);
    }

    @Override
    public Platillo save(@NonNull Platillo platillo) {
        log.info("Saving platillo: {}", platillo);
        return platilloRepository.save(platillo);
    }

    @Override
    public List<Platillo> findAll() {
        log.info("Retrieving all platillos from database.");
        return platilloRepository.findAll();
    }

    @Override
    public void deleteIngrediente(@NonNull Long id) {
        log.info("Deleting ingrediente with id: {}", id);
        platilloRepository.deleteIngrediente(id);
    }

}
