package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoDocumentoOrdenCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompraDocumentoResponseDTO {
    private Long id;
    private TipoDocumentoOrdenCompra tipoDocumento;
    private String nombreVisible;
    private String contentType;
    private Long size;
    private LocalDateTime fechaCreacion;
    private String creadoPorNombre;
}
