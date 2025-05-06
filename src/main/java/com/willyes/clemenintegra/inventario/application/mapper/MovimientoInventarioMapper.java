package com.willyes.clemenintegra.inventario.application.mapper;

import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.domain.model.*;

public class MovimientoInventarioMapper {

    public static MovimientoInventario toEntity(MovimientoInventarioDTO dto) {
        return MovimientoInventario.builder()
                .cantidad(dto.cantidad())
                .tipoMovimiento(dto.tipoMovimiento())
                .docReferencia(dto.docReferencia())
                .producto(new Producto(dto.productoId()))
                .lote(new LoteProducto(dto.loteId()))
                .almacen(new Almacen(dto.almacenId()))
                .proveedor(new Proveedor(dto.proveedorId()))
                .ordenCompra(new OrdenCompra(dto.ordenCompraId()))
                .motivoMovimiento(new MotivoMovimiento(dto.motivoMovimientoId()))
                .tipoMovimientoDetalle(dto.tipoMovimientoDetalle())
                .registradoPor(new Usuario(dto.registradoPorId()))
                .build();
    }

    public static MovimientoInventarioDTO toDTO(MovimientoInventario movimiento) {
        return new MovimientoInventarioDTO(
                movimiento.getCantidad(),
                movimiento.getTipoMovimiento(),
                movimiento.getDocReferencia(),
                movimiento.getProducto() != null ? movimiento.getProducto().getId() : null,
                movimiento.getLote() != null ? movimiento.getLote().getId() : null,
                movimiento.getAlmacen() != null ? movimiento.getAlmacen().getId() : null,
                movimiento.getProveedor() != null ? movimiento.getProveedor().getId() : null,
                movimiento.getOrdenCompra() != null ? movimiento.getOrdenCompra().getId() : null,
                movimiento.getMotivoMovimiento() != null ? movimiento.getMotivoMovimiento().getId() : null,
                movimiento.getTipoMovimientoDetalle(),
                movimiento.getRegistradoPor() != null ? movimiento.getRegistradoPor().getId() : null
        );
    }


}

