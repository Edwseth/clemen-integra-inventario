package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;

import java.math.BigDecimal;

@Data
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
    private TipoAnalisisCalidad tipoAnalisis = TipoAnalisisCalidad.NINGUNO;

    @NotNull
    private Long unidadMedidaId;

    @NotNull
    private Long categoriaProductoId;

    public TipoAnalisisCalidad getTipoAnalisis() {return tipoAnalisis;}
    public void setTipoAnalisis(TipoAnalisisCalidad tipoAnalisis) {this.tipoAnalisis = tipoAnalisis;}
}
