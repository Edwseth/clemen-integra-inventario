package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraConDetallesResponse;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrdenCompraMapperResumenTest {

    private final OrdenCompraMapper mapper = Mappers.getMapper(OrdenCompraMapper.class);

    @Test
    void totalsAreCalculatedInResponses() {
        OrdenCompraDetalle d1 = OrdenCompraDetalle.builder()
                .id(1L)
                .cantidad(new BigDecimal("10"))
                .cantidadRecibida(new BigDecimal("4"))
                .valorUnitario(BigDecimal.ONE)
                .valorTotal(BigDecimal.TEN)
                .iva(BigDecimal.ZERO)
                .build();
        OrdenCompraDetalle d2 = OrdenCompraDetalle.builder()
                .id(2L)
                .cantidad(new BigDecimal("5"))
                .cantidadRecibida(new BigDecimal("5"))
                .valorUnitario(BigDecimal.ONE)
                .valorTotal(BigDecimal.ONE)
                .iva(BigDecimal.ZERO)
                .build();

        OrdenCompra orden = OrdenCompra.builder()
                .id(10)
                .codigoOrden("OC-1")
                .estado(EstadoOrdenCompra.ENVIADA)
                .fechaOrden(LocalDateTime.now())
                .fechaCompromisoEntrega(LocalDate.now().plusDays(1))
                .descuento(BigDecimal.ZERO)
                .detalles(List.of(d1, d2))
                .build();

        OrdenCompraResponseDTO dto = mapper.toDTO(orden);
        assertEquals(new BigDecimal("15"), dto.getTotalPedido());
        assertEquals(new BigDecimal("9"), dto.getTotalRecibido());
        assertEquals(new BigDecimal("6"), dto.getTotalPendiente());
        assertEquals(new BigDecimal("60.00"), dto.getPorcentajeAvance());
    }

    @Test
    void detailPendingIsPresent() {
        OrdenCompraDetalle detalle = OrdenCompraDetalle.builder()
                .id(3L)
                .cantidad(new BigDecimal("3"))
                .cantidadRecibida(BigDecimal.ONE)
                .valorUnitario(BigDecimal.ONE)
                .valorTotal(BigDecimal.ONE)
                .iva(BigDecimal.ZERO)
                .build();
        OrdenCompra orden = OrdenCompra.builder()
                .id(11)
                .estado(EstadoOrdenCompra.ENVIADA)
                .fechaOrden(LocalDateTime.now())
                .detalles(List.of(detalle))
                .build();

        OrdenCompraConDetallesResponse dto = mapper.toOrdenCompraConDetallesResponse(orden);
        assertEquals(new BigDecimal("2"), dto.detalles.get(0).cantidadPendiente);
    }
}

