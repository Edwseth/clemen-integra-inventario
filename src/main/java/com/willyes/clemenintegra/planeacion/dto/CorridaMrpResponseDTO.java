package com.willyes.clemenintegra.planeacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorridaMrpResponseDTO {
    private Long id;
    private Long planId;
    private LocalDateTime fechaEjecucion;
    private LocalDate horizonteInicio;
    private LocalDate horizonteFin;
    private String estado;
    private String versionFormulaUsada;
    private List<DetalleCorridaMrpDTO> detalles;
    private List<SugerenciaAbastecimientoDTO> sugerencias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleCorridaMrpDTO {
        private Long id;
        private Long productoId;
        private String productoSku;
        private String productoNombre;
        private String codigoInsumo;
        private String nombreInsumo;
        private String categoriaInsumo;
        private BigDecimal requerimientoBruto;
        private BigDecimal inventarioDisponible;
        private BigDecimal recepcionesProgramadas;
        private BigDecimal requerimientoNeto;
        private Integer nivelBom;
        private String mensajeValidacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SugerenciaAbastecimientoDTO {
        private Long id;
        private Long detalleCorridaId;
        private Long productoId;
        private String productoSku;
        private String productoNombre;
        private String tipo;
        private BigDecimal cantidadSugerida;
        private LocalDate fechaNecesidad;
        private LocalDate fechaSugeridaLanzamiento;
        private Integer leadTimeDias;
        private String estado;
    }
}
