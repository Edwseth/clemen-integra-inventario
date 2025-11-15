package com.willyes.clemenintegra.produccion.service.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class DistribucionFefoResult {
    private final Long productoInsumoId;
    private final BigDecimal requerido;
    private final BigDecimal stockFisicoTotal;
    private final BigDecimal stockReservadoTotal;
    private final BigDecimal stockLibreTotal;
    private final BigDecimal faltante;
    private final boolean suficiente;
    private final boolean usoFallback;
    private final List<Long> almacenesPreferidos;
    private final List<DistribucionFefoDetalle> detalles;

    public List<DistribucionFefoDetalle> getDetalles() {
        return detalles == null ? List.of() : Collections.unmodifiableList(detalles);
    }
}
