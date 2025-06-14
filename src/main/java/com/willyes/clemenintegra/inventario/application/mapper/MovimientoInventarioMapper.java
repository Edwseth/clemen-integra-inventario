package com.willyes.clemenintegra.inventario.application.mapper;

import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoInventarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaIngreso", ignore = true)
    @Mapping(target = "producto", source = "productoId")
    @Mapping(target = "lote", source = "loteId")
    @Mapping(target = "almacen", source = "almacenId")
    @Mapping(target = "proveedor", source = "proveedorId")
    @Mapping(target = "ordenCompra", source = "ordenCompraId")
    @Mapping(target = "motivoMovimiento", source = "motivoMovimientoId")
    @Mapping(target = "registradoPor", source = "registradoPorId")
    MovimientoInventario toEntity(MovimientoInventarioDTO dto);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "loteId", source = "lote.id")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "ordenCompraId", source = "ordenCompra.id")
    @Mapping(target = "motivoMovimientoId", source = "motivoMovimiento.id")
    @Mapping(target = "registradoPorId", source = "registradoPor.id")
    MovimientoInventarioDTO toDTO(MovimientoInventario movimiento);

    default Producto map(Long id) {
        return id == null ? null : new Producto(id);
    }

    default LoteProducto mapLote(Long id) {
        return id == null ? null : new LoteProducto(id);
    }

    default Almacen mapAlmacen(Long id) {
        return id == null ? null : new Almacen(id);
    }

    default Proveedor mapProveedor(Long id) {
        return id == null ? null : new Proveedor(id);
    }

    default OrdenCompra mapOrden(Long id) {
        return id == null ? null : new OrdenCompra(id);
    }

    default MotivoMovimiento mapMotivo(Long id) {
        return id == null ? null : new MotivoMovimiento(id);
    }

    default Usuario mapUsuario(Long id) {
        return id == null ? null : new Usuario(id);
    }
}
