package com.willyes.clemenintegra.produccion.mapper;

import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.springframework.stereotype.Component;

@Component
public class OrdenProduccionMapper {

    public OrdenProduccion toEntity(OrdenProduccionRequestDTO dto, Producto producto, Usuario responsable) {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setLoteProduccion(dto.getLoteProduccion());
        orden.setFechaInicio(dto.getFechaInicio());
        orden.setFechaFin(dto.getFechaFin());
        orden.setCantidadProgramada(dto.getCantidadProgramada());
        orden.setCantidadProducida(dto.getCantidadProducida());
        orden.setEstado(EstadoProduccion.valueOf(dto.getEstado()));
        orden.setProducto(producto);
        orden.setResponsable(responsable);
        return orden;
    }

}

