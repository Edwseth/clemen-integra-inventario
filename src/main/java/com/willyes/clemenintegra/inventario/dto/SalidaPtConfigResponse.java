package com.willyes.clemenintegra.inventario.dto;

public record SalidaPtConfigResponse(
        boolean enabled,
        Long almacenPtId,
        Long tipoDetalleSalidaPtId
) {
}
