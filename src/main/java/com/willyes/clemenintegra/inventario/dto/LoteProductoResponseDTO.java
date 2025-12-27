package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteProductoResponseDTO {
    private Long id;
    private String codigoLote;
    private Long ordenProduccionId;
    private String codigoOrdenProduccion;
    private LocalDateTime fechaFabricacion;
    private LocalDateTime fechaVencimiento;
    private BigDecimal stockLote;
    private EstadoLote estado;
    private Double temperaturaAlmacenamiento;
    private LocalDateTime fechaLiberacion;
    private String nombreProducto;
    private Long productoId;
    private String tipoAnalisisCalidad;
    private String estadoCalidadResumen;
    private Long plantillaMicroId;
    private boolean requiereAnalisisMicro;
    private boolean requiereAnalisisFisico;
    private boolean requiereAnalisisQuimico;
    private boolean requiereAnalisisMicrobiologico;
    private boolean tieneEvaluacionFisica;
    private boolean tieneEvaluacionQuimicoMicro;
    private Long evaluacionQuimicoMicroId;
    private boolean tieneResultadosMicro;
    private boolean pendienteFisico;
    private boolean pendienteQuimico;
    private boolean pendienteMicro;
    private String nombreAlmacen;
    private String ubicacionAlmacen;
    private String nombreUsuarioLiberador;
    private List<TipoEvaluacion> evaluaciones;
    private Long lotePsOrigenId;
    private String codigoLotePsOrigen;
    private String alerta;
    private String codigoUbicacionInterna;
    private String descripcionUbicacionInterna;
    private Long ubicacionFisicaId;
    private String ubicacionFisicaCodigo;
    private String ubicacionFisicaDescripcion;

    public String getNombreProducto() {
        return nombreProducto;}
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;}
    public String getNombreAlmacen() {
        return nombreAlmacen;}
    public void setNombreAlmacen(String nombreAlmacen) {
        this.nombreAlmacen = nombreAlmacen;}
    public String getUbicacionAlmacen() {
        return ubicacionAlmacen;}
    public void setUbicacionAlmacen(String ubicacionAlmacen) {
        this.ubicacionAlmacen = ubicacionAlmacen;}
    public String getNombreUsuarioLiberador() {
        return nombreUsuarioLiberador;}
    public void setNombreUsuarioLiberador(String nombreUsuarioLiberador) {
        this.nombreUsuarioLiberador = nombreUsuarioLiberador;}
}
