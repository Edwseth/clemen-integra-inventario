package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoDetalleResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResumenResponseDTO;
import com.willyes.clemenintegra.inventario.model.ConteoCiclico;
import com.willyes.clemenintegra.inventario.model.ConteoCiclicoDetalle;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ConteoCiclicoMapper {

    public ConteoCiclicoResumenResponseDTO toResumen(ConteoCiclico conteo) {
        if (conteo == null) {
            return null;
        }
        return ConteoCiclicoResumenResponseDTO.builder()
                .id(conteo.getId())
                .almacenId(conteo.getAlmacen() != null ? conteo.getAlmacen().getId() : null)
                .estado(conteo.getEstado())
                .fechaCreacion(conteo.getFechaCreacion())
                .aplicadoEn(conteo.getAplicadoEn())
                .creadoPorId(conteo.getCreadoPor() != null ? conteo.getCreadoPor().getId() : null)
                .aplicadoPorId(conteo.getAplicadoPor() != null ? conteo.getAplicadoPor().getId() : null)
                .build();
    }

    public ConteoCiclicoResponseDTO toResponseCompleto(ConteoCiclico conteo) {
        if (conteo == null) {
            return null;
        }
        return ConteoCiclicoResponseDTO.builder()
                .id(conteo.getId())
                .almacenId(conteo.getAlmacen() != null ? conteo.getAlmacen().getId() : null)
                .estado(conteo.getEstado())
                .fechaCreacion(conteo.getFechaCreacion())
                .aplicadoEn(conteo.getAplicadoEn())
                .creadoPorId(conteo.getCreadoPor() != null ? conteo.getCreadoPor().getId() : null)
                .aplicadoPorId(conteo.getAplicadoPor() != null ? conteo.getAplicadoPor().getId() : null)
                .detalles(toDetalles(conteo.getDetalles()))
                .build();
    }

    private List<ConteoCiclicoDetalleResponseDTO> toDetalles(List<ConteoCiclicoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return Collections.emptyList();
        }
        return detalles.stream()
                .filter(Objects::nonNull)
                .map(this::toDetalle)
                .collect(Collectors.toList());
    }

    private ConteoCiclicoDetalleResponseDTO toDetalle(ConteoCiclicoDetalle detalle) {
        return ConteoCiclicoDetalleResponseDTO.builder()
                .id(detalle.getId())
                .productoId(detalle.getProducto() != null && detalle.getProducto().getId() != null
                        ? detalle.getProducto().getId().longValue()
                        : null)
                .loteProductoId(detalle.getLoteProducto() != null ? detalle.getLoteProducto().getId() : null)
                .loteCodigo(detalle.getLoteProducto() != null ? detalle.getLoteProducto().getCodigoLote() : null)
                .ubicacionFisicaId(detalle.getUbicacionFisica() != null ? detalle.getUbicacionFisica().getId() : null)
                .stockSistema(detalle.getStockSistema())
                .conteoFisico(detalle.getConteoFisico())
                .diferencia(detalle.getDiferencia())
                .build();
    }
}
