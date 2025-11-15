package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO utilizado para exponer solicitudes de movimiento. Los campos de cabecera relacionados con
 * producto, lote y almacenes pueden provenir del primer detalle cuando la entidad original tiene
 * cabecera vacía (caso habitual en solicitudes generadas desde Producción). El frontend puede
 * apoyarse en estos identificadores para precargar formularios de movimientos asociados a una OP.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudMovimientoResponseDTO {
    private Long id;
    private TipoMovimiento tipoMovimiento;
    private Integer productoId;
    private String nombreProducto;
    private String codigoSku;     // SKU del producto para prefill
    private Long loteProductoId;
    private String nombreLote;
    private BigDecimal cantidad;
    private Integer almacenOrigenId;
    private String nombreAlmacenOrigen;
    private Integer almacenDestinoId;
    private String nombreAlmacenDestino;
    private Long ordenProduccionId;
    private String codigoOrden;   // Código de la OP (si aplica) para banner/prefill
    private Long motivoMovimientoId;
    private Long tipoMovimientoDetalleId;
    private String nombreSolicitante;
    private String nombreResponsable;
    private EstadoSolicitudMovimiento estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String observaciones;
    private List<SolicitudMovimientoDetalleDTO> detalles;
}
