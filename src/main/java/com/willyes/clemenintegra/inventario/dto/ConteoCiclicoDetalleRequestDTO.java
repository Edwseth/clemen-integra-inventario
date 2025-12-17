package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConteoCiclicoDetalleRequestDTO {

    @NotNull
    private Long productoId;
    @NotNull(message = "Debe seleccionar un lote")
    private Long loteProductoId;
    @JsonAlias("ubicacionId")
    private Long ubicacionFisicaId;

    private BigDecimal stockSistema;

    @NotNull
    @DecimalMin(value = "0.00", message = "El conteo físico no puede ser negativo")
    private BigDecimal conteoFisico;
}
