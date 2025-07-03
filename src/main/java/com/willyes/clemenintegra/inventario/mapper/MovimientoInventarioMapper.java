package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MovimientoInventarioMapper {

    // Convertir DTO plano a entidad ignorando relaciones complejas
    //@Mapping(target = "producto", ignore = true)
    //@Mapping(target = "lote", ignore = true)
    //@Mapping(target = "almacen", ignore = true)
    //@Mapping(target = "proveedor", ignore = true)
    //@Mapping(target = "ordenCompra", ignore = true)
    //@Mapping(target = "motivoMovimiento", ignore = true)
    //@Mapping(target = "tipoMovimientoDetalle", ignore = true)
    //@Mapping(target = "registradoPor", ignore = true)
    //@Mapping(target = "ordenCompraDetalle", ignore = true)
    //@Mapping(target = "fechaIngreso", ignore = true)
    MovimientoInventario toEntity(MovimientoInventarioDTO dto);

    // Convertir entidad a DTO básico (para uso interno)
    @Mapping(target = "productoId", expression = "java(movimiento.getProducto() != null ? movimiento.getProducto().getId() : null)")
    @Mapping(target = "loteProductoId", expression = "java(movimiento.getLote() != null ? movimiento.getLote().getId() : null)")
    @Mapping(target = "almacenId", expression = "java(movimiento.getAlmacen() != null ? movimiento.getAlmacen().getId() : null)")
    @Mapping(target = "proveedorId", expression = "java(movimiento.getProveedor() != null ? movimiento.getProveedor().getId() : null)")
    @Mapping(target = "ordenCompraId", expression = "java(movimiento.getOrdenCompra() != null ? movimiento.getOrdenCompra().getId() : null)")
    @Mapping(target = "motivoMovimientoId", expression = "java(movimiento.getMotivoMovimiento() != null ? movimiento.getMotivoMovimiento().getId() : null)")
    @Mapping(target = "tipoMovimientoDetalleId", expression = "java(movimiento.getTipoMovimientoDetalle() != null ? movimiento.getTipoMovimientoDetalle().getId() : null)")
    @Mapping(target = "ordenCompraDetalleId", expression = "java(movimiento.getOrdenCompraDetalle() != null ? movimiento.getOrdenCompraDetalle().getId() : null)")
    MovimientoInventarioDTO toDTO(MovimientoInventario movimiento);

    // Convertir a DTO de respuesta (más completo para vistas)
    @Mapping(target = "tipoMovimiento", source = "tipoMovimiento")
    @Mapping(target = "nombreProducto", expression = "java(movimiento.getProducto() != null ? movimiento.getProducto().getNombre() : null)")
    @Mapping(target = "nombreLote", expression = "java(movimiento.getLote() != null ? movimiento.getLote().getCodigoLote() : null)")
    @Mapping(target = "nombreAlmacen", expression = "java(movimiento.getAlmacen() != null ? movimiento.getAlmacen().getNombre() : null)")
    @Mapping(target = "nombreMotivo", expression = "java(movimiento.getMotivoMovimiento() != null && movimiento.getMotivoMovimiento().getMotivo() != null ? movimiento.getMotivoMovimiento().getMotivo().name() : null)")
    @Mapping(target = "tipoAlmacen", expression = "java(movimiento.getAlmacen() != null && movimiento.getAlmacen().getTipo() != null ? movimiento.getAlmacen().getTipo().name() : null)")
    @Mapping(target = "fechaIngreso", source = "fechaIngreso")
    @Mapping(target = "nombreUsuario", expression = "java(movimiento.getRegistradoPor() != null ? movimiento.getRegistradoPor().getNombreCompleto() : null)")
    MovimientoInventarioResponseDTO toResponseDTO(MovimientoInventario movimiento);

}

