package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.dto.EtapaPlantillaReordenRequest;
import java.util.List;

public interface EtapaPlantillaService {
    List<EtapaPlantilla> listarPorProducto(Integer productoId);
    List<EtapaPlantilla> preview(Integer productoId);
    EtapaPlantilla crear(EtapaPlantilla etapa);
    EtapaPlantilla actualizar(Long id, EtapaPlantilla etapa);
    void eliminar(Long id);
    void reordenar(Integer productoId, List<EtapaPlantillaReordenRequest> cambios);
}
