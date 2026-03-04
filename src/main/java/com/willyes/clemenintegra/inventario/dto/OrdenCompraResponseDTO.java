package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponseDTO {
    private Long id;
    private String codigoOrden;
    private String estado;
    private String tipo;
    private String proveedorNombre;
    private LocalDateTime fechaOrden;
    private LocalDate fechaCompromisoEntrega;
    private BigDecimal descuento;
    private BigDecimal totalPedido;
    private BigDecimal totalRecibido;
    private BigDecimal totalPendiente;
    private BigDecimal porcentajeAvance;

    public OrdenCompraResponseDTO(Integer id,
                                  String codigoOrden,
                                  EstadoOrdenCompra estado,
                                  TipoOrdenCompra tipo,
                                  String proveedorNombre,
                                  LocalDateTime fechaOrden,
                                  LocalDate fechaCompromisoEntrega,
                                  BigDecimal descuento,
                                  BigDecimal totalPedido,
                                  BigDecimal totalRecibido) {
        this.id = id != null ? id.longValue() : null;
        this.codigoOrden = codigoOrden;
        this.estado = estado != null ? estado.name() : null;
        this.tipo = tipo != null ? tipo.name() : null;
        this.proveedorNombre = proveedorNombre;
        this.fechaOrden = fechaOrden;
        this.fechaCompromisoEntrega = fechaCompromisoEntrega;
        this.descuento = descuento;
        this.totalPedido = totalPedido != null ? totalPedido : BigDecimal.ZERO;
        this.totalRecibido = totalRecibido != null ? totalRecibido : BigDecimal.ZERO;
        this.totalPendiente = calcularPendiente(this.totalPedido, this.totalRecibido);
        this.porcentajeAvance = calcularPorcentaje(this.totalPedido, this.totalRecibido);
    }

    private BigDecimal calcularPendiente(BigDecimal totalPedido, BigDecimal totalRecibido) {
        BigDecimal pendiente = totalPedido.subtract(totalRecibido);
        return pendiente.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pendiente;
    }

    private BigDecimal calcularPorcentaje(BigDecimal totalPedido, BigDecimal totalRecibido) {
        if (totalPedido.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalRecibido.multiply(BigDecimal.valueOf(100))
                .divide(totalPedido, 2, java.math.RoundingMode.HALF_UP);
    }

    public String getEstado() {return estado;}
    public String getProveedorNombre() {return proveedorNombre;}
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCodigoOrden() {return codigoOrden;}
    public void setCodigoOrden(String codigoOrden) {this.codigoOrden = codigoOrden;}
    public void setProveedorNombre(String proveedorNombre) {this.proveedorNombre = proveedorNombre;}
    public void setEstado(String estado) {this.estado = estado;}
    public LocalDateTime getFechaOrden() {return fechaOrden;}
    public void setFechaOrden(LocalDateTime fechaOrden) {this.fechaOrden = fechaOrden;}
    public BigDecimal getDescuento() {return descuento;}
    public void setDescuento(BigDecimal descuento) {this.descuento = descuento;}
}
