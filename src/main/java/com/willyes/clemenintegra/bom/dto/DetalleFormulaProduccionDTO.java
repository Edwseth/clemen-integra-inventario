package com.willyes.clemenintegra.bom.dto;

import java.math.BigDecimal;

public class DetalleFormulaProduccionDTO {
    public Long detalleId;
    public Long productoInsumoId;
    public String codigoInsumo;
    public String nombreInsumo;
    public Long unidadMedidaId;
    public String nombreUnidadMedida;
    public String simboloUnidadMedida;
    public BigDecimal cantidadNecesaria;
    public boolean obligatorio;
}
