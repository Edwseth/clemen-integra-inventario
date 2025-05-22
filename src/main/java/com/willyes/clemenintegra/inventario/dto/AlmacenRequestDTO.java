package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import lombok.*;

@Getter
@Setter
public class AlmacenRequestDTO {
    private String nombre;
    private String ubicacion;
    private TipoCategoria categoria;
    private TipoAlmacen tipo;
}
