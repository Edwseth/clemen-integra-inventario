package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("sku")
    @JsonAlias("codigoSku")
    private String sku;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 255)
    private String descripcionProducto;

    @NotNull
    private BigDecimal stockMinimo;

    private BigDecimal stockMinimoProveedor;

    @Builder.Default
    @Pattern(regexp = "NINGUNO|FISICO_QUIMICO|MICROBIOLOGICO|AMBOS")
    private String tipoAnalisisCalidad = TipoAnalisisCalidad.NINGUNO.name();

    @NotNull
    private Long unidadMedidaId;

    @NotNull
    private Long categoriaProductoId;

    public String getTipoAnalisisCalidad() {return tipoAnalisisCalidad;}
    public void setTipoAnalisisCalidad(String tipoAnalisisCalidad) {this.tipoAnalisisCalidad = tipoAnalisisCalidad;}
}
