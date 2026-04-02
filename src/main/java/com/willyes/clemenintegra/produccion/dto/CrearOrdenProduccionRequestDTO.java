package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearOrdenProduccionRequestDTO {

    @NotNull
    private Long productoId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cantidadProgramada;

    @NotNull
    @FutureOrPresent
    private LocalDateTime fechaProgramada;

    @NotNull
    private Long responsableId;

    private Long planDetalleId;

    private Long lotePsId;

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
