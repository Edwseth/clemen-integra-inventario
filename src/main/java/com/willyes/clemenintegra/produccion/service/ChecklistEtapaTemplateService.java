package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;

import java.util.List;

public interface ChecklistEtapaTemplateService {
    List<ChecklistEtapaTemplate> listarActivosPorEtapaPlantilla(Long etapaPlantillaId);
}
