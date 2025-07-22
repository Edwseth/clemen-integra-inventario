package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponseDTO> listarTodos(Pageable pageable);
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
