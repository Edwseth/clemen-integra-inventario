package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.*;
import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;

import java.util.List;

public interface PlantillasAnalisisService {
    List<PlantillaAnalisisResumenDTO> listarPorProductoYTipo(Long productoId, TipoAnalisisPlantilla tipoAnalisis);

    PlantillaAnalisisDetalleDTO obtenerDetalle(Long plantillaId);

    PlantillaAnalisisDetalleDTO crearPlantilla(Long productoId, TipoAnalisisPlantilla tipoAnalisis, PlantillaAnalisisCreateRequest request);

    PlantillaAnalisisDetalleDTO clonarComoNuevaVersion(Long plantillaId);

    PlantillaAnalisisDetalleDTO marcarVigente(Long plantillaId);

    PlantillaCampoDTO crearCampo(Long plantillaId, PlantillaCampoRequest request);

    PlantillaCampoDTO editarCampo(Long plantillaId, Long campoId, PlantillaCampoRequest request);

    void eliminarCampo(Long plantillaId, Long campoId);
}
