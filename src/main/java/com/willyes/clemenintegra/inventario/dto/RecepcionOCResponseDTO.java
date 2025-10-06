package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionOCResponseDTO {

    private Long id;
    private String codigo;
    private LocalDate fechaRecepcion;
    private Integer ordenCompraId;
    private String codigoOrdenCompra;
    private Integer almacenDestinoId;
    private String nombreAlmacenDestino;
    private Integer proveedorId;
    private String nombreProveedor;
    private Long usuarioId;
    private String nombreUsuario;
    private String observaciones;

    @Builder.Default
    private List<RecepcionOCDetalleResponseDTO> detalles = Collections.emptyList();

    @Builder.Default
    private List<MovimientoInventarioResponseDTO> movimientos = Collections.emptyList();
}

