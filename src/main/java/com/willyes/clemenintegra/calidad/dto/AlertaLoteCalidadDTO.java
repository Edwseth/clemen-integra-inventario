package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoAlertaLoteCalidad;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaLoteCalidadDTO {

    private Long loteId;
    private String codigoLote;
    private Long productoId;
    private String codigoSku;
    private String nombreProducto;
    private Long almacenId;
    private String nombreAlmacen;
    private EstadoLote estadoLote;
    private LocalDateTime fechaVencimiento;
    private Integer diasParaVencer;
    private TipoAlertaLoteCalidad tipoAlerta;
}
