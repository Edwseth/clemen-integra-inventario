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
    @Mapping(target = "solicitudMovimiento", ignore = true)
    @Mapping(target = "clasificacion", source = "clasificacionMovimientoInventario")
    MovimientoInventario toEntity(MovimientoInventarioDTO dto);

    // Convertir entidad a DTO básico (para uso interno)
    @Mapping(target = "productoId", expression = "java(movimiento.getProducto() != null ? movimiento.getProducto().getId() : null)")
    @Mapping(target = "loteProductoId", expression = "java(movimiento.getLote() != null ? movimiento.getLote().getId() : null)")
    @Mapping(target = "almacenOrigenId", expression = "java(movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getId() : null)")
    @Mapping(target = "almacenDestinoId", expression = "java(movimiento.getAlmacenDestino() != null ? movimiento.getAlmacenDestino().getId() : null)")
    @Mapping(target = "proveedorId", expression = "java(movimiento.getProveedor() != null ? movimiento.getProveedor().getId() : null)")
    @Mapping(target = "ordenCompraId", expression = "java(movimiento.getOrdenCompra() != null ? movimiento.getOrdenCompra().getId() : null)")
    @Mapping(target = "motivoMovimientoId", expression = "java(movimiento.getMotivoMovimiento() != null ? movimiento.getMotivoMovimiento().getId() : null)")
    @Mapping(target = "tipoMovimientoDetalleId", expression = "java(movimiento.getTipoMovimientoDetalle() != null ? movimiento.getTipoMovimientoDetalle().getId() : null)")
    @Mapping(target = "ordenCompraDetalleId", expression = "java(movimiento.getOrdenCompraDetalle() != null ? movimiento.getOrdenCompraDetalle().getId() : null)")
    @Mapping(target = "solicitudMovimientoId", expression = "java(movimiento.getSolicitudMovimiento() != null ? movimiento.getSolicitudMovimiento().getId() : null)")
    @Mapping(target = "clasificacionMovimientoInventario", source = "clasificacion")
    MovimientoInventarioDTO toDTO(MovimientoInventario movimiento);

    // Conversión segura para respuesta evitando ciclos y proxys
    default MovimientoInventarioResponseDTO safeToResponseDTO(MovimientoInventario m) {
        if (m == null) return null;
        MovimientoInventarioResponseDTO dto = new MovimientoInventarioResponseDTO();
        dto.setId(m.getId());
        dto.setFechaIngreso(m.getFechaIngreso());
        dto.setTipoMovimiento(m.getTipoMovimiento());

        String clasificacion = "-";
        if (m.getClasificacion() != null) {
            clasificacion = m.getClasificacion().name();
        } else if (m.getMotivoMovimiento() != null && m.getMotivoMovimiento().getMotivo() != null) {
            clasificacion = m.getMotivoMovimiento().getMotivo().name();
        }
        dto.setClasificacion(clasificacion);

        dto.setCantidad(m.getCantidad());

        var p = m.getProducto();
        dto.setProductoId(p != null ? (p.getId() == null ? null : Long.valueOf(p.getId())) : null);
        dto.setNombreProducto(p != null ? p.getNombre() : null);
        dto.setSku(p != null ? p.getCodigoSku() : null);

        var l = m.getLote();
        dto.setLoteId(l != null ? l.getId() : null);
        dto.setCodigoLote(l != null ? l.getCodigoLote() : null);

        var ao = m.getAlmacenOrigen();
        dto.setNombreAlmacenOrigen(ao != null ? ao.getNombre() : null);

        var ad = m.getAlmacenDestino();
        dto.setNombreAlmacenDestino(ad != null ? ad.getNombre() : null);

        var u = m.getRegistradoPor();
        dto.setNombreUsuarioRegistrador(u != null ? u.getNombreCompleto() : null);

        return dto;
    }
}

