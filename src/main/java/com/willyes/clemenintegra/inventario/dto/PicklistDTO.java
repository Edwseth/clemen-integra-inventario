package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PicklistDTO {
    private String codigoOrden;
    private byte[] archivo;
}
