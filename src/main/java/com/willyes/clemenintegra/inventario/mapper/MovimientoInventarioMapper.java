package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import org.springframework.stereotype.Component;

@Component
public class MovimientoInventarioMapper {

    public static MovimientoInventario toEntity(MovimientoInventarioDTO dto) {
        MovimientoInventario m = new MovimientoInventario();
        m.setCantidad(dto.cantidad());
        m.setTipoMovimiento(dto.tipoMovimiento());
        m.setDocReferencia(dto.docReferencia());
        // Nota: No creamos aquí Producto, Almacén, Motivo, etc.
        return m;
    }

    public static MovimientoInventarioDTO toDTO(MovimientoInventario movimiento) {
        return new MovimientoInventarioDTO(
                movimiento.getId(),
                movimiento.getCantidad(),
                movimiento.getTipoMovimiento(),
                movimiento.getDocReferencia(),
                movimiento.getProducto().getId(),
                movimiento.getLote() != null ? movimiento.getLote().getId() : null,
                movimiento.getAlmacen().getId(),
                movimiento.getProveedor() != null ? movimiento.getProveedor().getId() : null,
                movimiento.getOrdenCompra() != null ? movimiento.getOrdenCompra().getId() : null,
                movimiento.getMotivoMovimiento().getId(),
                movimiento.getTipoMovimientoDetalle().getId(),
                movimiento.getRegistradoPor().getId(),
                movimiento.getOrdenCompraDetalle() != null ? movimiento.getOrdenCompraDetalle().getId() : null
        );
    }

    public MovimientoInventarioResponseDTO toResponseDTO(MovimientoInventario movimiento) {
        return new MovimientoInventarioResponseDTO(
                movimiento.getId(),
                movimiento.getCantidad(),
                movimiento.getProducto().getId(),
                movimiento.getTipoMovimiento().name(),
                movimiento.getProducto().getNombre(),
                movimiento.getLote().getCodigoLote(),
                movimiento.getAlmacen().getNombre()
        );
    }


}
