package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public class AtencionDTO {

    private Long detalleId;
    private Long loteId;
    private BigDecimal cantidad;
    private Integer almacenOrigenId;
    private Integer almacenDestinoId;

    public Long getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(Long detalleId) {
        this.detalleId = detalleId;
    }

    public Long getLoteId() {
        return loteId;
    }

    public void setLoteId(Long loteId) {
        this.loteId = loteId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getAlmacenOrigenId() {
        return almacenOrigenId;
    }

    public void setAlmacenOrigenId(Integer almacenOrigenId) {
        this.almacenOrigenId = almacenOrigenId;
    }

    public Integer getAlmacenDestinoId() {
        return almacenDestinoId;
    }

    public void setAlmacenDestinoId(Integer almacenDestinoId) {
        this.almacenDestinoId = almacenDestinoId;
    }
}
