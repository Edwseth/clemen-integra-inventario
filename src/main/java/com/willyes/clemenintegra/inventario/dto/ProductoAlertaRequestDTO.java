package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoAlertaRequestDTO {

    // Opcional: dejar preparado para agregar filtros más adelante
    @Size(max = 50)
    private String categoria;   // ej. MATERIA_PRIMA
    private Boolean soloActivos;
}

