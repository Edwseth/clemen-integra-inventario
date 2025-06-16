package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T18:54:38-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class MovimientoInventarioMapperImpl implements MovimientoInventarioMapper {

    @Override
    public MovimientoInventario toEntity(MovimientoInventarioDTO dto) {
        if ( dto == null ) {
            return null;
        }

        MovimientoInventario movimientoInventario = new MovimientoInventario();

        return movimientoInventario;
    }

    @Override
    public MovimientoInventarioDTO toDTO(MovimientoInventario movimiento) {
        if ( movimiento == null ) {
            return null;
        }

        Long productoId = movimiento.getProducto() != null ? movimiento.getProducto().getId() : null;
        Long loteProductoId = movimiento.getLote() != null ? movimiento.getLote().getId() : null;
        Long almacenId = movimiento.getAlmacen() != null ? movimiento.getAlmacen().getId() : null;
        Long proveedorId = movimiento.getProveedor() != null ? movimiento.getProveedor().getId() : null;
        Long ordenCompraId = movimiento.getOrdenCompra() != null ? movimiento.getOrdenCompra().getId() : null;
        Long motivoMovimientoId = movimiento.getMotivoMovimiento() != null ? movimiento.getMotivoMovimiento().getId() : null;
        Long tipoMovimientoDetalleId = movimiento.getTipoMovimientoDetalle() != null ? movimiento.getTipoMovimientoDetalle().getId() : null;
        Long usuarioId = movimiento.getRegistradoPor() != null ? movimiento.getRegistradoPor().getId() : null;
        Long ordenCompraDetalleId = movimiento.getOrdenCompraDetalle() != null ? movimiento.getOrdenCompraDetalle().getId() : null;
        Long id = null;
        BigDecimal cantidad = null;
        ClasificacionMovimientoInventario tipoMovimiento = null;
        String docReferencia = null;

        MovimientoInventarioDTO movimientoInventarioDTO = new MovimientoInventarioDTO( id, cantidad, tipoMovimiento, docReferencia, productoId, loteProductoId, almacenId, proveedorId, ordenCompraId, motivoMovimientoId, tipoMovimientoDetalleId, usuarioId, ordenCompraDetalleId );

        return movimientoInventarioDTO;
    }

    @Override
    public MovimientoInventarioResponseDTO toResponseDTO(MovimientoInventario movimiento) {
        if ( movimiento == null ) {
            return null;
        }

        Long productoId = movimiento.getProducto() != null ? movimiento.getProducto().getId() : null;
        String tipoMovimiento = convertTipoMovimiento(movimiento.getTipoMovimiento());
        String nombreProducto = movimiento.getProducto() != null ? movimiento.getProducto().getNombre() : null;
        String nombreLote = movimiento.getLote() != null ? movimiento.getLote().getCodigoLote() : null;
        String nombreAlmacen = movimiento.getAlmacen() != null ? movimiento.getAlmacen().getNombre() : null;
        Long id = null;
        BigDecimal cantidad = null;

        MovimientoInventarioResponseDTO movimientoInventarioResponseDTO = new MovimientoInventarioResponseDTO( id, cantidad, productoId, tipoMovimiento, nombreProducto, nombreLote, nombreAlmacen );

        return movimientoInventarioResponseDTO;
    }
}
