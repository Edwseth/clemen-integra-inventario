package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.RecepcionOCDetalleResponseDTO;
import com.willyes.clemenintegra.inventario.dto.RecepcionOCResponseDTO;
import com.willyes.clemenintegra.inventario.model.RecepcionOC;
import com.willyes.clemenintegra.inventario.model.RecepcionOCDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecepcionOCMapper {

    @Mapping(target = "ordenCompraId", source = "ordenCompra.id")
    @Mapping(target = "codigoOrdenCompra", source = "ordenCompra.codigoOrden")
    @Mapping(target = "almacenDestinoId", source = "almacenDestino.id")
    @Mapping(target = "nombreAlmacenDestino", source = "almacenDestino.nombre")
    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "nombreProveedor", source = "proveedor.nombre")
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "nombreUsuario", source = "usuario.nombreCompleto")
    @Mapping(target = "detalles", source = "detalles")
    RecepcionOCResponseDTO toResponse(RecepcionOC recepcion);

    @Mapping(target = "ordenCompraDetalleId", source = "ordenCompraDetalle.id")
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "productoSku", source = "producto.codigoSku")
    @Mapping(target = "loteId", source = "lote.id")
    @Mapping(target = "codigoLote", source = "lote.codigoLote")
    RecepcionOCDetalleResponseDTO toDetalle(RecepcionOCDetalle detalle);

    default RecepcionOCResponseDTO toResponse(RecepcionOC recepcion, List<MovimientoInventarioResponseDTO> movimientos) {
        RecepcionOCResponseDTO response = toResponse(recepcion);
        response.setMovimientos(movimientos);
        return response;
    }
}

