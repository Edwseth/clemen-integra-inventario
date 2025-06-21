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

    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public String getSimbolo() {return simbolo;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setId(Long id) {this.id = id;}
    public void setSimbolo(String simbolo) {this.simbolo = simbolo;}
}
