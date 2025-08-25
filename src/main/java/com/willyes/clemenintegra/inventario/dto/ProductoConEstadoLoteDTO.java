package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoConEstadoLoteDTO {
    private Long productoId;
    @JsonProperty("sku")
    @JsonAlias("codigoSku")
    private String sku;
    private String nombre;
    private String estadoLote;

    @JsonProperty("codigoSku")
    public String getCodigoSku() {return sku;}
}

