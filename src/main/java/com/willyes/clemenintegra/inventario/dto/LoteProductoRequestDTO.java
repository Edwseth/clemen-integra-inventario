package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteProductoRequestDTO {
    @NotBlank
    private String codigoLote;
    @PastOrPresent
    private LocalDateTime fechaFabricacion;
    @Future
    private LocalDateTime fechaVencimiento;
    @NotNull
    @Positive
    private BigDecimal stockLote;
    /**
     * Estado inicial enviado por el cliente.
     * Este valor se ignora al crear el lote ya que
     * el servicio establece el estado automáticamente.
     */
    private EstadoLote estado;
    private Double temperaturaAlmacenamiento;
    private LocalDateTime fechaLiberacion;
    @NotNull
    private Long productoId;
    @NotNull
    private Long almacenId;
    private Long usuario_liberador_id;
    private Long orden_produccion_id;
    private Long produccion_id;
}

