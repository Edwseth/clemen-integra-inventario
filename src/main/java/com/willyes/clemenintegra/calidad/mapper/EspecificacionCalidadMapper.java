package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.EspecificacionCalidadDTO;
import com.willyes.clemenintegra.calidad.model.EspecificacionCalidad;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.springframework.stereotype.Component;

@Component
public class EspecificacionCalidadMapper {

    public EspecificacionCalidadDTO toDTO(EspecificacionCalidad entity) {
        return EspecificacionCalidadDTO.builder()
                .id(entity.getId())
                .parametro(entity.getParametro())
                .valorMinimo(entity.getValorMinimo())
                .valorMaximo(entity.getValorMaximo())
                .metodoEnsayo(entity.getMetodoEnsayo())
                .productoId(entity.getProducto().getId().longValue())
                .build();
    }

    public EspecificacionCalidad toEntity(EspecificacionCalidadDTO dto, Producto producto) {
        return EspecificacionCalidad.builder()
                .id(dto.getId())
                .parametro(dto.getParametro())
                .valorMinimo(dto.getValorMinimo())
                .valorMaximo(dto.getValorMaximo())
                .metodoEnsayo(dto.getMetodoEnsayo())
                .producto(producto)
                .build();
    }
}

