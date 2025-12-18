package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface OrdenCompraMapper {

    default OrdenCompra toEntity(OrdenCompraRequestDTO dto, Proveedor proveedor, EstadoOrdenCompra estado) {
        OrdenCompra entity = new OrdenCompra();
        entity.setProveedor(proveedor);
        entity.setCondicionesPago(dto.getCondicionesPago());
        entity.setComprador(dto.getComprador());
        entity.setEstado(estado);
        entity.setFechaOrden(java.time.LocalDateTime.now());
        entity.setObservaciones(dto.getObservaciones());
        entity.setFechaCompromisoEntrega(dto.getFechaCompromisoEntrega());
        if (dto.getDescuento() != null) {
            entity.setDescuento(dto.getDescuento());
        }
        return entity;
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "codigoOrden", source = "codigoOrden")
    @Mapping(target = "estado", source = "estado", qualifiedByName = "enumName")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    @Mapping(target = "fechaOrden", source = "fechaOrden")
    @Mapping(target = "fechaCompromisoEntrega", source = "fechaCompromisoEntrega")
    @Mapping(target = "descuento", source = "descuento")
    @Mapping(target = "totalPedido", expression = "java(resumen(orden).get(\"totalPedido\"))")
    @Mapping(target = "totalRecibido", expression = "java(resumen(orden).get(\"totalRecibido\"))")
    @Mapping(target = "totalPendiente", expression = "java(resumen(orden).get(\"totalPendiente\"))")
    @Mapping(target = "porcentajeAvance", expression = "java(resumen(orden).get(\"porcentajeAvance\"))")
    OrdenCompraResponseDTO toDTO(OrdenCompra orden);

    @org.mapstruct.Named("enumName")
    default String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    @Mapping(target = "proveedor", source = "proveedor")
    @Mapping(target = "detalles", source = "detalles")
    @Mapping(target = "fechaOrden", source = "fechaOrden")
    @Mapping(target = "descuento", source = "descuento")
    @Mapping(target = "fechaCompromisoEntrega", source = "fechaCompromisoEntrega")
    @Mapping(target = "totalPedido", expression = "java(resumen(orden).get(\"totalPedido\"))")
    @Mapping(target = "totalRecibido", expression = "java(resumen(orden).get(\"totalRecibido\"))")
    @Mapping(target = "totalPendiente", expression = "java(resumen(orden).get(\"totalPendiente\"))")
    @Mapping(target = "porcentajeAvance", expression = "java(resumen(orden).get(\"porcentajeAvance\"))")
    OrdenCompraConDetallesResponse toOrdenCompraConDetallesResponse(OrdenCompra orden);

    ProveedorMinResponse toProveedorMin(Proveedor proveedor);
    ProveedorResponseDTO toProveedorDTO(Proveedor proveedor);

    @Mapping(target = "producto", source = "producto", qualifiedByName = "mapProductoMini")
    @Mapping(target = "fechaNecesidad", source = "fechaNecesidad")
    @Mapping(target = "cantidadPendiente", expression = "java(pendiente(detalle))")
    OrdenCompraDetalleResponse toOrdenCompraDetalleResponse(OrdenCompraDetalle detalle);

    List<OrdenCompraDetalleResponse> toDetalleList(List<OrdenCompraDetalle> detalles);

    @Named("mapDetalleList")
    default List<OrdenCompraDetalleResponse> mapDetalleList(List<OrdenCompraDetalle> detalles) {
        return toDetalleList(detalles);
    }

    @Named("mapProductoMini")
    default ProductoMiniDTO mapProductoMini(Producto producto) {
        if (producto == null) return null;

        String udmSimbolo = null;
        if (producto.getUnidadMedida() != null) {
            String imp = producto.getUnidadMedida().getSimboloImpresion(); // mL, kg, m...
            String sim = producto.getUnidadMedida().getSimbolo();          // ML, KG, M...
            udmSimbolo = (imp != null && !imp.isBlank()) ? imp : sim;
        }

        UnidadMiniDTO unidadDTO = new UnidadMiniDTO(udmSimbolo);
        return new ProductoMiniDTO(
                producto.getId().longValue(),
                producto.getNombre(),
                unidadDTO
        );
    }

    default BigDecimal pendiente(OrdenCompraDetalle detalle) {
        if (detalle == null) return BigDecimal.ZERO;
        BigDecimal solicitada = detalle.getCantidad() != null ? detalle.getCantidad() : BigDecimal.ZERO;
        BigDecimal recibida = detalle.getCantidadRecibida() != null ? detalle.getCantidadRecibida() : BigDecimal.ZERO;
        BigDecimal pendiente = solicitada.subtract(recibida);
        return pendiente.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pendiente;
    }

    default Map<String, BigDecimal> resumen(OrdenCompra orden) {
        BigDecimal totalPedido = BigDecimal.ZERO;
        BigDecimal totalRecibido = BigDecimal.ZERO;
        if (orden != null && orden.getDetalles() != null) {
            for (OrdenCompraDetalle d : orden.getDetalles()) {
                if (d != null) {
                    totalPedido = totalPedido.add(d.getCantidad() != null ? d.getCantidad() : BigDecimal.ZERO);
                    totalRecibido = totalRecibido.add(d.getCantidadRecibida() != null ? d.getCantidadRecibida() : BigDecimal.ZERO);
                }
            }
        }
        BigDecimal totalPendiente = totalPedido.subtract(totalRecibido);
        if (totalPendiente.compareTo(BigDecimal.ZERO) < 0) {
            totalPendiente = BigDecimal.ZERO;
        }
        BigDecimal porcentajeAvance = totalPedido.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalRecibido.multiply(BigDecimal.valueOf(100))
                .divide(totalPedido, 2, java.math.RoundingMode.HALF_UP);
        return Map.of(
                "totalPedido", totalPedido,
                "totalRecibido", totalRecibido,
                "totalPendiente", totalPendiente,
                "porcentajeAvance", porcentajeAvance
        );
    }
}

