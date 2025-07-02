package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ajustes_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(length = 100, nullable = false)
    private String motivo;

    @Column(length = 255)
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ajuste_producto"))
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "almacenes_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ajuste_almacen"))
    private Almacen almacen;

    @ManyToOne
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ajuste_usuario"))
    private Usuario usuario;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public LocalDateTime getFecha() {return fecha;}
    public void setFecha(LocalDateTime fecha) {this.fecha = fecha;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getMotivo() {return motivo;}
    public void setMotivo(String motivo) {this.motivo = motivo;}
    public String getObservaciones() {return observaciones;}
    public void setObservaciones(String observaciones) {this.observaciones = observaciones;}
    public Producto getProducto() {return producto;}
    public void setProducto(Producto producto) {this.producto = producto;}
    public Almacen getAlmacen() {return almacen;}
    public void setAlmacen(Almacen almacen) {this.almacen = almacen;}
    public Usuario getUsuario() {return usuario;}
    public void setUsuario(Usuario usuario) {this.usuario = usuario;}
}

