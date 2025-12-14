package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexItemDTO {

    private LocalDateTime fechaMovimiento;
    private String tipoMovimiento;
    private String clasificacion;
    private String referencia;
    private String almacenOrigen;
    private String almacenDestino;
    private String codigoLote;
    private String codigoSku;
    private String nombreProducto;
    private BigDecimal cantidadEntrada;
    private BigDecimal cantidadSalida;
    private BigDecimal saldo;
    private String usuario;
}
