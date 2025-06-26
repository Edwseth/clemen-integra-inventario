package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnidadMiniDTO {
    private String simbolo;

    public String getSimbolo() {return simbolo;}
    public void setSimbolo(String simbolo) {this.simbolo = simbolo;}
}