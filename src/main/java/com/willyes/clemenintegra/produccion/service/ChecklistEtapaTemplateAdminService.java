package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateRequest;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateResponse;

import java.util.List;

public interface ChecklistEtapaTemplateAdminService {
    List<ChecklistEtapaTemplateResponse> listar(Long etapaPlantillaId);

    ChecklistEtapaTemplateResponse crear(Long etapaPlantillaId, ChecklistEtapaTemplateRequest request);

    ChecklistEtapaTemplateResponse actualizar(Long id, ChecklistEtapaTemplateRequest request);

    void eliminar(Long id);

    List<ChecklistEtapaTemplateResponse> copiarDesde(Long etapaPlantillaId, Long origenEtapaPlantillaId);
}
