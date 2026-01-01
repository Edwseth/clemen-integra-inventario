package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaDTO;
import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;

import java.util.List;

public interface ChecklistEtapaService {
    ChecklistEtapaDTO obtenerPorEtapa(Long etapaId);
    ChecklistEtapaDTO obtenerPorOrdenYEtapa(Long ordenId, Long etapaId);
    ChecklistEtapaDTO actualizar(Long etapaId, List<ChecklistItemDTO> items);
    ChecklistEtapaDTO actualizarEnOrden(Long ordenId, Long etapaId, List<ChecklistItemDTO> items);
    byte[] exportCsvPorOrden(Long ordenId);
    void validarChecklistCompleto(Long etapaId);
}
