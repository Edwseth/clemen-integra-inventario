package com.willyes.clemenintegra.bom.model;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FormulaProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private String version;

    @Enumerated(EnumType.STRING)
    private EstadoFormula estado;

    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL)
    private List<DetalleFormula> detalles;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL)
    private List<DocumentoFormula> documentos;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Producto getProducto() {return producto;}
    public void setProducto(Producto producto) {this.producto = producto;}
    public String getVersion() {return version;}
    public void setVersion(String version) {this.version = version;}
    public EstadoFormula getEstado() {return estado;}
    public void setEstado(EstadoFormula estado) {this.estado = estado;}
    public LocalDateTime getFechaCreacion() {return fechaCreacion;}
    public void setFechaCreacion(LocalDateTime fechaCreacion) {this.fechaCreacion = fechaCreacion;}
    public Usuario getCreadoPor() {return creadoPor;}
    public void setCreadoPor(Usuario creadoPor) {this.creadoPor = creadoPor;}
    public List<DetalleFormula> getDetalles() {return detalles;}
    public void setDetalles(List<DetalleFormula> detalles) {this.detalles = detalles;}
    public List<DocumentoFormula> getDocumentos() {return documentos;}
    public void setDocumentos(List<DocumentoFormula> documentos) {this.documentos = documentos;}
}

