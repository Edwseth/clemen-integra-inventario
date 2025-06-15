package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoInventarioMapper {

    MovimientoInventario toEntity(MovimientoInventarioDTO dto);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "loteProductoId", source = "lote.id")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "ordenCompraId", source = "ordenCompra.id")
    @Mapping(target = "motivoMovimientoId", source = "motivoMovimiento.id")
    @Mapping(target = "tipoMovimientoDetalleId", source = "tipoMovimientoDetalle.id")
    @Mapping(target = "usuarioId", source = "registradoPor.id")
    @Mapping(target = "ordenCompraDetalleId", source = "ordenCompraDetalle.id")
    MovimientoInventarioDTO toDTO(MovimientoInventario movimiento);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "tipoMovimiento", expression = "java(movimiento.getTipoMovimiento().name())")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "nombreLote", source = "lote.codigoLote")
    @Mapping(target = "nombreAlmacen", source = "almacen.nombre")
    MovimientoInventarioResponseDTO toResponseDTO(MovimientoInventario movimiento);
}
