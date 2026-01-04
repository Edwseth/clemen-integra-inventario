package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistEtapaTemplateServiceImpl implements ChecklistEtapaTemplateService {

    private final ChecklistEtapaTemplateRepository repository;

    @Override
    public List<ChecklistEtapaTemplate> listarActivosPorEtapaPlantilla(Long etapaPlantillaId) {
        return repository.findByEtapaPlantillaIdAndActivoTrueOrderByOrdenAsc(etapaPlantillaId);
    }
}
