package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDetalleRequestDTO {
    private BigDecimal cantidad;
    private BigDecimal valorUnitario;
    private BigDecimal iva;
    private Long productoId;
    private Long ordenCompraId;

}
