package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponseDTO {
    private Long id;
    private LocalDateTime fechaIngreso;
    private TipoMovimiento tipoMovimiento;
    private ClasificacionMovimientoInventario clasificacion;
    private BigDecimal cantidad;
    private Long productoId;
    private String nombreProducto;
    private String codigoSku;
    private Long loteId;
    private String codigoLote;
    private String nombreAlmacenOrigen;
    private String nombreAlmacenDestino;
    private String nombreUsuarioRegistrador;

}

