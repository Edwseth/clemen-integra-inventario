package com.willyes.clemenintegra.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloqueanteDTO {
    private boolean insuficiente;
    private String motivo;
}
