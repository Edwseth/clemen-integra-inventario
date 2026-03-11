package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.*;

import java.util.List;

public interface EspecificacionesCalidadService {
    EspecificacionesFisicoQuimicasProductoDTO listarFisicoQuimicasPorProducto(Long productoId);

    List<EspecificacionFisicoQuimicaDTO> crearFisicoQuimicas(Long productoId, List<EspecificacionFisicoQuimicaRequest> requests);

    EspecificacionFisicoQuimicaDTO actualizarFisicoQuimica(Long id, EspecificacionFisicoQuimicaRequest request);

    void eliminarFisicoQuimica(Long id);

    List<PlantillaAnalisisMicrobiologicoResumenDTO> listarPlantillasMicroPorProducto(Long productoId);

    List<PlantillaMicrobiologicaReutilizableDTO> listarTodasLasPlantillasMicro();

    PlantillaAnalisisMicrobiologicoDetalleDTO obtenerDetallePlantillaMicro(Long plantillaId);

    PlantillaAnalisisMicrobiologicoDetalleDTO crearPlantillaMicro(Long productoId, PlantillaAnalisisMicrobiologicoRequest request);

    PlantillaAnalisisMicrobiologicoDetalleDTO clonarPlantillaMicroComoNuevaVersion(Long plantillaId);

    PlantillaAnalisisMicrobiologicoDetalleDTO clonarPlantillaMicroParaProducto(Long plantillaId, Long productoId);
}
