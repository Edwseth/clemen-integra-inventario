package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ParametroAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PlantillaAnalisisMicroServiceImpl implements PlantillaAnalisisMicroService {

    private final ProductoRepository productoRepository;

    @Override
    public PlantillaAnalisisMicroDTO obtenerPorProducto(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado con ID: " + productoId));

        if (producto.getTipoAnalisisCalidad() == null ||
                (producto.getTipoAnalisisCalidad() != TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO
                        && producto.getTipoAnalisisCalidad() != TipoAnalisisCalidad.AMBOS)) {
            return null;
        }

        PlantillaAnalisisMicrobiologico plantilla = producto.getPlantillaAnalisisMicrobiologico();
        if (plantilla == null) {
            return null;
        }

        return PlantillaAnalisisMicroDTO.builder()
                .id(plantilla.getId())
                .nombre(plantilla.getNombre())
                .descripcion(plantilla.getDescripcion())
                .activo(plantilla.isActivo())
                .parametros(plantilla.getParametros().stream()
                        .map(p -> ParametroAnalisisMicroDTO.builder()
                                .id(p.getId())
                                .nombreEnsayo(p.getNombreEnsayo())
                                .unidad(p.getUnidad())
                                .especificacion(p.getEspecificacion())
                                .tipoResultado(p.getTipoResultado())
                                .orden(p.getOrden())
                                .build())
                        .toList())
                .build();
    }
}

