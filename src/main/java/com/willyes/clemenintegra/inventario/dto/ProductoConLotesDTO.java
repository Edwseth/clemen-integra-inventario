package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoConLotesDTO {
    private Long productoId;
    @JsonProperty("sku")
    @JsonAlias("codigoSku")
    private String sku;
    private String nombre;
    private List<LoteSimpleDTO> lotes;

    @JsonProperty("codigoSku")
    public String getCodigoSku() {return sku;}
}
