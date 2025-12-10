package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ParametroAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PlantillaAnalisisMicroServiceImpl implements PlantillaAnalisisMicroService {

    private final ProductoRepository productoRepository;

    /**
     * El backend decide si corresponde mostrar la tabla microbiológica y entrega los parámetros
     * únicamente cuando se cumplen las condiciones de negocio.
     */
    @Transactional(readOnly = true)
    @Override
    public PlantillaAnalisisMicroDTO obtenerPorProducto(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado con ID: " + productoId));

        PlantillaAnalisisMicrobiologico plantilla = producto.getPlantillaAnalisisMicrobiologico();
        boolean requiereAnalisisMicro = requiereAnalisisMicro(producto, plantilla);

        if (!requiereAnalisisMicro) {
            return PlantillaAnalisisMicroDTO.builder()
                    .requiereAnalisisMicro(false)
                    .build();
        }

        return PlantillaAnalisisMicroDTO.builder()
                .id(plantilla.getId())
                .nombre(plantilla.getNombre())
                .descripcion(plantilla.getDescripcion())
                .activo(plantilla.isActivo())
                .requiereAnalisisMicro(true)
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

    boolean requiereAnalisisMicro(Producto producto, PlantillaAnalisisMicrobiologico plantilla) {
        TipoAnalisisCalidad tipo = producto.getTipoAnalisisCalidad();
        boolean tipoRequiereMicro = tipo == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO
                || tipo == TipoAnalisisCalidad.AMBOS;
        boolean plantillaValida = plantilla != null
                && plantilla.getParametros() != null
                && !plantilla.getParametros().isEmpty();
        return tipoRequiereMicro && plantillaValida;
    }
}

