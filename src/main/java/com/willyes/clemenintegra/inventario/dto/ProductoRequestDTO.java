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
    @Pattern(regexp = "NINGUNO|FISICO|QUIMICO_MICROBIOLOGICO|AMBOS")
    private String tipoAnalisisCalidad = TipoAnalisisCalidad.NINGUNO.name();

    @NotNull
    private Long unidadMedidaId;

    @NotNull
    private Long categoriaProductoId;

    /**
     * Sólo aplica a PT con SKU que inicia en 'PT'.
     * En otros casos, el backend lo ignorará (se forzará a null).
     */
    @Digits(integer = 36, fraction = 2, message = "Máximo 36 enteros y 2 decimales")
    @DecimalMin(value = "0.00", message = "Debe ser >= 0.00")
    private BigDecimal rendimientoUnidad;

    private Integer leadTimeCompraDias;

    private Integer leadTimeProduccionDias;

    @Digits(integer = 19, fraction = 6, message = "Máximo 19 enteros y 6 decimales")
    @DecimalMin(value = "0.00", message = "Debe ser >= 0.00")
    private BigDecimal stockSeguridad;

    @Digits(integer = 19, fraction = 6, message = "Máximo 19 enteros y 6 decimales")
    @DecimalMin(value = "0.00", message = "Debe ser >= 0.00")
    private BigDecimal stockMaximoPlaneacion;

    private Long plantillaAnalisisMicroId;

    private Boolean requiereAnalisisFisico;
    private Boolean requiereAnalisisQuimico;
    private Boolean requiereAnalisisMicrobiologico;

    public String getTipoAnalisisCalidad() {return tipoAnalisisCalidad;}
    public void setTipoAnalisisCalidad(String tipoAnalisisCalidad) {this.tipoAnalisisCalidad = tipoAnalisisCalidad;}
}
