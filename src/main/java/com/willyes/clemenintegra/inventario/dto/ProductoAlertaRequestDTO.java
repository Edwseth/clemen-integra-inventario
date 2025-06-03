package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoAlertaRequestDTO {

    // Opcional: dejar preparado para agregar filtros más adelante
    private String categoria;   // ej. MATERIA_PRIMA
    private Boolean soloActivos;
}

