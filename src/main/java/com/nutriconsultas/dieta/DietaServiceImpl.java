package com.nutriconsultas.dieta;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class DietaServiceImpl implements DietaService {
    @Autowired
    private DietaRepository dietaRepository;

    @Override
    public Dieta getDieta(@NonNull Long id) {
        log.info("Getting dieta with id: " + id);
        return dietaRepository.findById(id).orElse(null);
    }

    @Override
    public Dieta saveDieta(@NonNull Dieta dieta) {
        log.info("Saving dieta with id: " + dieta.getId());
        return dietaRepository.save(dieta);
    }

    @Override
    public void deleteDieta(@NonNull Long id) {
        log.info("Deleting dieta with id: " + id);
        dietaRepository.deleteById(id);
    }

    @Override
    public List<Dieta> getDietas() {
        log.info("Getting all dietas");
        return dietaRepository.findAll();
    }

    @Override
    public void addIngesta(Long id, String nombreIngesta) {
        log.info("Adding ingesta to dieta with id: " + id);
        Dieta dieta = dietaRepository.findById(id).orElse(null);
        if (dieta != null) {
            Ingesta ingesta = new Ingesta();
            ingesta.setNombre(nombreIngesta);
            ingesta.setDieta(dieta);
            dieta.getIngestas().add(ingesta);
            dietaRepository.save(dieta);
        }
    }
}
