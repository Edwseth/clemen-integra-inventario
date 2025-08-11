package com.willyes.clemenintegra.bom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoteResumenDTO {
    private Long idLote;
    private String codigoLote;
    private EstadoLote estado;
    private String almacenNombre;
    private BigDecimal stockLote;
    private LocalDateTime fechaVencimiento;
    private LocalDateTime fechaLiberacion;
    private String nombreUsuarioLiberador;
}
