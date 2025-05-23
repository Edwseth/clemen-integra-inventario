package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.Almacen;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlmacenResponseDTO {
    private Long id;
    private String nombre;
    private String ubicacion;
    private String categoria;
    private String tipo;

    public AlmacenResponseDTO(Almacen almacen) {
        this.id = almacen.getId();
        this.nombre = almacen.getNombre();
        this.ubicacion = almacen.getUbicacion();
        this.categoria = almacen.getCategoria().name();
        this.tipo = almacen.getTipo().name();
    }
}
