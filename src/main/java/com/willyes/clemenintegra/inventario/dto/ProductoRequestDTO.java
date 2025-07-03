package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoRequestDTO {

    @NotBlank
    @Size(max = 50)
    private String codigoSku;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 255)
    private String descripcionProducto;

    @NotNull
    private BigDecimal stockMinimo;

    private BigDecimal stockMinimoProveedor;

    @Builder.Default
    private boolean requiereInspeccion = false;

    @NotNull
    private Long unidadMedidaId;

    @NotNull
    private Long categoriaProductoId;

    public boolean isRequiereInspeccion() {return requiereInspeccion;}
    public void setRequiereInspeccion(boolean requiereInspeccion) {this.requiereInspeccion = requiereInspeccion;}
}
