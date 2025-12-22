package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspecificacionesFisicoQuimicasProductoDTO {
    private Long productoId;
    private String codigoSku;
    private String nombreProducto;
    private List<EspecificacionFisicoQuimicaDTO> parametros;
}
