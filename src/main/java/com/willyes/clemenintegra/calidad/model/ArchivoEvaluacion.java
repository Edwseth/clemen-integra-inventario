package com.willyes.clemenintegra.calidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoEvaluacion {

    @Column(name = "archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "nombre_visible", length = 100)
    private String nombreVisible;
}
