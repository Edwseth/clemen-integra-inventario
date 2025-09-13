package com.willyes.clemenintegra.produccion.dto;

import com.willyes.clemenintegra.inventario.dto.AlmacenResponseDTO;
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
public class LoteProductoResponse {
    private Long id;
    private String codigoLote;
    private LocalDateTime fechaFabricacion;
    private LocalDateTime fechaVencimiento;
    private EstadoLote estado;
    private AlmacenResponseDTO almacen;
}

