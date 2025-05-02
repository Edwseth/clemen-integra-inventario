package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.domain.enums.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProductoRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    private TipoCategoria tipo;
}
