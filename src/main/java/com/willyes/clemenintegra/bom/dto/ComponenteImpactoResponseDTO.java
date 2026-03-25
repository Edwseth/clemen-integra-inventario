package com.willyes.clemenintegra.bom.dto;

import java.util.List;

public class ComponenteImpactoResponseDTO {
    public ProductoImpactoDTO producto;
    public ResumenImpactoDTO resumen;
    public List<FormulaImpactoReferenciaDTO> formulasActivas;
    public List<FormulaImpactoReferenciaDTO> formulasHistoricas;

    public static class ProductoImpactoDTO {
        public Long id;
        public String sku;
        public String nombre;
        public Boolean activo;
    }

    public static class ResumenImpactoDTO {
        public int referenciasActivas;
        public int referenciasHistoricas;
        public int totalReferencias;
        public boolean puedeInactivarse;
        public String motivoBloqueo;
    }
}
