package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteProductoResponseDTO {
    private Long id;
    private String codigoLote;
    private LocalDateTime fechaFabricacion;
    private LocalDateTime fechaVencimiento;
    private BigDecimal stockLote;
    private EstadoLote estado;
    private Double temperaturaAlmacenamiento;
    private LocalDateTime fechaLiberacion;
    private String nombreProducto;
    private String nombreAlmacen;
    private String nombreUsuarioLiberador;

    public String getNombreProducto() {
        return nombreProducto;}
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;}
    public String getNombreAlmacen() {
        return nombreAlmacen;}
    public void setNombreAlmacen(String nombreAlmacen) {
        this.nombreAlmacen = nombreAlmacen;}
    public String getNombreUsuarioLiberador() {
        return nombreUsuarioLiberador;}
    public void setNombreUsuarioLiberador(String nombreUsuarioLiberador) {
        this.nombreUsuarioLiberador = nombreUsuarioLiberador;}
}
