package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

public interface ProductoService {
    List<ProductoResponseDTO> listarTodos();
    List<ProductoResponseDTO> buscarPorCategoria(String categoria);
    ProductoResponseDTO crearProducto(ProductoRequestDTO dto);
    ProductoResponseDTO obtenerPorId(Long id);
    ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO dto);
    UnidadMedidaResponseDTO cambiarUnidadMedida(Long productoId, UnidadMedidaRequestDTO dto);
    void eliminarProducto(Long id);
    List<ProductoConEstadoLoteDTO> buscarProductosConLotesPorEstado(String estado);
    List<ProductoConLotesDTO> buscarProductosConLotesAgrupadosPorEstado(String estado);
    Workbook generarReporteStockActualExcel();
}
