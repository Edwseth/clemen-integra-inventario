package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrdenCompraPdfDTO {

    public static class EmpresaDTO {
        public String nombre;      // LABORATORIO CLEMEN SAS
        public String nit;         // 901626440
        public String direccion;   // Carrera 41 D # 46-40 Union de Vivienda...
        public String telefono;    // Teléfono / Movil 3175081762
    }

    public static class ProveedorDTO {
        public String nombre;      // LIDAGAS S.A E.S.P
        public String nit;         // opcional
        public String direccion;   // opcional
        public String ciudad;      // opcional
        public String telefono;    // opcional
        public String paginaWeb;   // opcional
    }

    public static class ItemDTO {
        public String codigo;        // SKU o código
        public String descripcion;   // nombre del producto
        public String fechaNecesidad; // texto (si aplica)
        public String udm;           // unidad (Kg, Galones…)
        public BigDecimal cantidad;
        public BigDecimal precioUnitario;
        public BigDecimal iva;       // % o valor
        public BigDecimal icui;      // si no aplica, 0
        public BigDecimal total;     // cantidad * precio + impuestos (si corresponde)
    }

    public EmpresaDTO empresa;
    public String numero;            // p.ej. OC-706561-0-0
    public LocalDateTime fecha;          // fecha de la OC
    public String condicionesPago;   // CONTADO | 30 DÍAS | 60 DÍAS (texto final)
    public String observaciones;     // libre
    public String comprador;         // “Pablo_Perez”
    public ProveedorDTO proveedor;
    public List<ItemDTO> items;
    public BigDecimal subtotal;
    public BigDecimal valorIva;
    public BigDecimal valorIcui;
    public BigDecimal total;
}

