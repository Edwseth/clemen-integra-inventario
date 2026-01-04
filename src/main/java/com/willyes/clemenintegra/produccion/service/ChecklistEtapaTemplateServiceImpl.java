package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
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
        return repository.findActiveByEtapaPlantillaIdOrderByOrdenAsc(etapaPlantillaId);
    }

    @Override
    public void crearPlaceholderPorDefectoSiNoExiste(EtapaPlantilla etapaPlantilla) {
        if (etapaPlantilla == null || etapaPlantilla.getId() == null) {
            return;
        }
        boolean existeActivo = repository.existsByEtapaPlantillaIdAndActivoTrue(etapaPlantilla.getId());
        if (existeActivo) {
            return;
        }
        ChecklistEtapaTemplate placeholder = ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaPlantilla)
                .nombreItem(PLACEHOLDER_NOMBRE)
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(1)
                .activo(true)
                .build();
        repository.save(placeholder);
    }

    @Override
    public void eliminarPorEtapaPlantilla(Long etapaPlantillaId) {
        if (etapaPlantillaId == null) {
            return;
        }
        List<ChecklistEtapaTemplate> templates = repository.findByEtapaPlantillaId(etapaPlantillaId);
        if (templates.isEmpty()) {
            return;
        }
        repository.deleteAll(templates);
    }
}
