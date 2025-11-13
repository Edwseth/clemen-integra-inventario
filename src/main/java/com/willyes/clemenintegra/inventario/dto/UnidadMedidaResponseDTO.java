package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadMedidaResponseDTO {
    private Long id;
    private String nombre;
    private String simbolo;
    private String nombrePlural;
    private String simboloImpresion;

    public UnidadMedidaResponseDTO(Long id, String nombre, String simbolo) {
        this.id = id;
        this.nombre = nombre;
        this.simbolo = simbolo;
    }


    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public String getSimbolo() {return simbolo;}
    public String getNombrePlural() { return nombrePlural; }
    public String getSimboloImpresion() { return simboloImpresion; }
    public void setNombrePlural(String v) { this.nombrePlural = v; }
    public void setSimboloImpresion(String v) { this.simboloImpresion = v; }
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setId(Long id) {this.id = id;}
    public void setSimbolo(String simbolo) {this.simbolo = simbolo;}
}
