package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCalidadUpdateDTO {

    @NotNull
    private Boolean requiereAnalisisFisico;

    @NotNull
    private Boolean requiereAnalisisQuimico;

    @NotNull
    private Boolean requiereAnalisisMicrobiologico;
}
