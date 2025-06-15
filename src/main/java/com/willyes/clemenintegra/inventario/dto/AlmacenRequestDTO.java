package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlmacenRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 255)
    private String ubicacion;

    @NotNull
    private TipoCategoria categoria;

    @NotNull
    private TipoAlmacen tipo;
}
