package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenProduccionRequestDTO {
    @NotNull
    @PastOrPresent
    private LocalDateTime fechaInicio;

    @NotNull
    @FutureOrPresent
    private LocalDateTime fechaFin;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cantidadProgramada;

    @DecimalMin(value = "0.0")
    private BigDecimal cantidadProducida;

    @NotBlank
    private String estado;

    @NotNull
    private Long productoId;

    @NotNull
    private Long responsableId;

    private Long lotePsId;

    /**
     * Símbolo de la unidad de medida en la que se expresa la cantidad programada
     * (por ejemplo, "kg" o "L"). Si se omite, se utilizará la unidad definida
     * para el producto.
     */
    @NotBlank
    private String unidadMedidaSimbolo;

    private Boolean confirmacionHomeopatico;

    @Size(max = 500, message = "motivoOverrideHomeopatico no debe superar 500 caracteres")
    private String motivoOverrideHomeopatico;

    @AssertTrue(message = "motivoOverrideHomeopatico es obligatorio cuando confirmacionHomeopatico=true y debe tener al menos 20 caracteres")
    public boolean isMotivoOverrideValidoCuandoConfirmado() {
        if (!Boolean.TRUE.equals(confirmacionHomeopatico)) {
            return true;
        }
        return motivoOverrideHomeopatico != null && motivoOverrideHomeopatico.trim().length() >= 20;
    }
}
