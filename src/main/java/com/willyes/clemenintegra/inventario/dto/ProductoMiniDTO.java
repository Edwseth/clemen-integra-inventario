package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoMiniDTO {
    private Long id;
    private String nombre;
    private UnidadMiniDTO unidadMedida;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public UnidadMiniDTO getUnidadMedida() {return unidadMedida;}
    public void setUnidadMedida(UnidadMiniDTO unidadMedida) {this.unidadMedida = unidadMedida;}
}
