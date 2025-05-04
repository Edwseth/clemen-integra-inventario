package com.willyes.clemenintegra.inventario.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

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
    private Integer stockActual;

    @NotNull
    private Integer stockMinimo;

    private Integer stockMinimoProveedor;

    private Boolean activo = true;

    private Boolean requiereInspeccion = false;

    @NotNull
    private Long unidadMedidaId;

    @NotNull
    private Long categoriaProductoId;

    @NotNull
    private Long usuarioId;
}
