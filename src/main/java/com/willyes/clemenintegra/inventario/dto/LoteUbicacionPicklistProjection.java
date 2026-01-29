package com.willyes.clemenintegra.inventario.dto;

public interface LoteUbicacionPicklistProjection {
    Integer getProductoId();
    Integer getAlmacenId();
    String getCodigoLote();
    String getUbicacionCodigo();
    String getUbicacionDescripcion();
}
