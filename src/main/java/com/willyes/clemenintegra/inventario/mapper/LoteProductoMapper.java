package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;

public class LoteProductoMapper {

    public static LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen) {
        LoteProducto lote = new LoteProducto();
        lote.setCodigoLote(dto.getCodigoLote());
        lote.setFechaFabricacion(dto.getFechaFabricacion());
        lote.setFechaVencimiento(dto.getFechaVencimiento());
        lote.setStockLote(dto.getStockLote());
        lote.setEstado(dto.getEstado());
        lote.setTemperaturaAlmacenamiento(dto.getTemperaturaAlmacenamiento());
        lote.setProducto(producto);
        lote.setAlmacen(almacen);
        return lote;
    }

    public static LoteProductoResponseDTO toDto(LoteProducto lote) {
        LoteProductoResponseDTO dto = new LoteProductoResponseDTO();
        dto.setId(lote.getId());
        dto.setCodigoLote(lote.getCodigoLote());
        dto.setFechaFabricacion(lote.getFechaFabricacion());
        dto.setFechaVencimiento(lote.getFechaVencimiento());
        dto.setStockLote(lote.getStockLote());
        dto.setEstado(lote.getEstado());
        dto.setTemperaturaAlmacenamiento(lote.getTemperaturaAlmacenamiento());
        dto.setFechaLiberacion(lote.getFechaLiberacion());
        dto.setNombreProducto(lote.getProducto().getNombre());
        dto.setNombreAlmacen(lote.getAlmacen().getNombre());
        return dto;
    }
}

