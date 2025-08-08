package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.DisponibilidadProductoResponseDTO;

public interface InventarioConsultaService {
    DisponibilidadProductoResponseDTO obtenerDisponibilidadPorProducto(Long productoId);
}
