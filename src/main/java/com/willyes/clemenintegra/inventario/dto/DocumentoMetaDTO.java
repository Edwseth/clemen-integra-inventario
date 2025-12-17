package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoDocumentoOrdenCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoMetaDTO {
    private String nombreVisible;
    private TipoDocumentoOrdenCompra tipoDocumento;
}
