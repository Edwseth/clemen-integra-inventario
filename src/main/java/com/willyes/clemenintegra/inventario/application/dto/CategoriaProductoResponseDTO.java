package com.willyes.clemenintegra.inventario.application.dto;

import com.willyes.clemenintegra.inventario.domain.enums.TipoCategoria;
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

