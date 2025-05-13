package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProductoResponseDTO {

    private Long id;
    private String nombre;
    private TipoCategoria tipo;
}

