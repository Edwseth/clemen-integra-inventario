package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudMovimientoRequestDTO {
    @NotNull
    private TipoMovimiento tipoMovimiento;
    @NotNull
    private Long productoId;
    private Long loteId;
    @NotNull
    private BigDecimal cantidad;
    private Long almacenOrigenId;
    private Long almacenDestinoId;
    private Long ordenProduccionId;
    @NotNull
    private Long usuarioSolicitanteId;
    private Long usuarioResponsableId;
    private String observaciones;
}
