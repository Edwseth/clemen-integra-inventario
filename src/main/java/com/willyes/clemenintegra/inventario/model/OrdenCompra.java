package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    private Long id;

    @Column(name = "codigo_orden", nullable = false, unique = true)
    private String codigoOrden;

    @Column(name = "fecha_orden", nullable = false)
    private LocalDate fechaOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orden_compra_proveedor"))
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 50, nullable = false)
    private EstadoOrdenCompra estado;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    public OrdenCompra(Long id) {
        this.id = id;
    }
    public Long getId() {return id;}
    public String getCodigoOrden() {return codigoOrden;}
    public LocalDate getFechaOrden() {return fechaOrden;}
    public Proveedor getProveedor() {return proveedor;}
    public EstadoOrdenCompra getEstado() {return estado;}
    public String getObservaciones() {return observaciones;}
    public void setId(Long id) {this.id = id;}
    public void setCodigoOrden(String codigoOrden) {this.codigoOrden = codigoOrden;}
    public void setFechaOrden(LocalDate fechaOrden) {this.fechaOrden = fechaOrden;}
    public void setProveedor(Proveedor proveedor) {this.proveedor = proveedor;}
    public void setEstado(EstadoOrdenCompra estado) {this.estado = estado;}
    public void setObservaciones(String observaciones) {this.observaciones = observaciones;}
}