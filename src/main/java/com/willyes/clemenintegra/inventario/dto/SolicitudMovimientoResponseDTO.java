package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudMovimientoResponseDTO {
    private Long id;
    private TipoMovimiento tipoMovimiento;
    private Integer productoId;
    private String nombreProducto;
    private Long loteProductoId;
    private String nombreLote;
    private BigDecimal cantidad;
    private Integer almacenOrigenId;
    private String nombreAlmacenOrigen;
    private Integer almacenDestinoId;
    private String nombreAlmacenDestino;
    private Long ordenProduccionId;
    private Long motivoMovimientoId;
    private Long tipoMovimientoDetalleId;
    private String nombreSolicitante;
    private String nombreResponsable;
    private EstadoSolicitudMovimiento estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String observaciones;
}
