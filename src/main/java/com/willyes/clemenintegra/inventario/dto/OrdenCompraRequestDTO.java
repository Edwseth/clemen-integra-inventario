package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.CondicionesPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraRequestDTO {

    @NotNull
    private Long proveedorId;
    @NotNull
    private CondicionesPago condicionesPago;
    @NotNull
    private String comprador;
    private String observaciones;
    @DecimalMin("0") @Digits(integer=10, fraction=2)
    private BigDecimal descuento;

    @NotEmpty
    private List<OrdenCompraDetalleRequestDTO> detalles;
}
