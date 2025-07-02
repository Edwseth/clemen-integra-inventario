package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotivoMovimientoResponseDTO {
    private Long id;
    private String motivo;

    public String getMotivo() {return motivo;}
    public void setMotivo(String motivo) {this.motivo = motivo;}
}

