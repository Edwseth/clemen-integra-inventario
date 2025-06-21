package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProductoResponseDTO {

    private Long id;
    private String nombre;
    private TipoCategoria tipo;

    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public TipoCategoria getTipo() {return tipo;}
    public void setId(Long id) {this.id = id;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setTipo(TipoCategoria tipo) {this.tipo = tipo;}
}

