package com.willyes.clemenintegra.inventario.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConteoAjusteReporteDTO(
        Long conteoId,
        LocalDateTime fechaConteo,
        LocalDateTime fechaAplicacion,
        Long almacenId,
        String almacenNombre,
        Long productoId,
        String productoSku,
        String productoNombre,
        Long loteId,
        String loteCodigo,
        BigDecimal stockAntes,
        BigDecimal conteoFisico,
        BigDecimal diferencia,
        String tipoAjuste,
        BigDecimal stockFinal,
        Long usuarioConteoId,
        String usuarioConteoNombre
) {
}
