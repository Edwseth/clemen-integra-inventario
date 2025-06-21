package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProveedorResponseDTO {
    private Long id;
    private String nombre;

    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public void setId(Long id) {this.id = id;}
    public void setNombre(String nombre) {this.nombre = nombre;}
}

