package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;

import java.util.List;

public interface ChecklistEtapaTemplateService {

    String PLACEHOLDER_NOMBRE = "Definir checklist operativo";

    List<ChecklistEtapaTemplate> listarActivosPorEtapaPlantilla(Long etapaPlantillaId);

    void crearPlaceholderPorDefectoSiNoExiste(EtapaPlantilla etapaPlantilla);

    void eliminarPorEtapaPlantilla(Long etapaPlantillaId);
}
