package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.CondicionesPago;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ordenes_compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "codigo_orden", nullable = false, unique = true)
    private String codigoOrden;

    @Column(name = "fecha_orden", nullable = false)
    private LocalDateTime fechaOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orden_compra_proveedor"))
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 50, nullable = false)
    private EstadoOrdenCompra estado;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Builder.Default
    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ordenCompra", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenCompraDetalle> detalles;

    @Enumerated(EnumType.STRING)
    @Column(name = "condiciones_pago", length = 20)
    private CondicionesPago condicionesPago;   // NUEVO

    @Column(name = "comprador", length = 100)
    private String comprador;


    public OrdenCompra(Integer id) {
        this.id = id;
    }
    public Integer getId() {return id;}
    public String getCodigoOrden() {return codigoOrden;}
    public LocalDateTime getFechaOrden() {return fechaOrden;}
    public Proveedor getProveedor() {return proveedor;}
    public EstadoOrdenCompra getEstado() {return estado;}
    public String getObservaciones() {return observaciones;}
    public List<OrdenCompraDetalle> getDetalles() {return detalles;}
    public CondicionesPago getCondicionesPago() { return condicionesPago; }
    public String getComprador() { return comprador; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public void setCondicionesPago(CondicionesPago condicionesPago) { this.condicionesPago = condicionesPago; }
    public void setComprador(String comprador) { this.comprador = comprador; }
    public void setDetalles(List<OrdenCompraDetalle> detalles) {this.detalles = detalles;}
    public void setId(Integer id) {this.id = id;}
    public void setCodigoOrden(String codigoOrden) {this.codigoOrden = codigoOrden;}
    public void setFechaOrden(LocalDateTime fechaOrden) {this.fechaOrden = fechaOrden;}
    public void setProveedor(Proveedor proveedor) {this.proveedor = proveedor;}
    public void setEstado(EstadoOrdenCompra estado) {this.estado = estado;}
    public void setObservaciones(String observaciones) {this.observaciones = observaciones;}
}