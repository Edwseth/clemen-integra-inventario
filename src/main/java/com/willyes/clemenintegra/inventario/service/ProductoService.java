package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponseDTO> listarTodos(Pageable pageable);
    List<ProductoResponseDTO> buscarPorCategoria(String categoria);
    List<ProductoResponseDTO> findByCategoriaTipo(String tipo);
    List<ProductoResponseDTO> findByCategoriaTipoIn(List<String> tipos);
    ProductoResponseDTO crearProducto(ProductoRequestDTO dto);
    ProductoResponseDTO obtenerPorId(Long id);
    ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO dto);
    UnidadMedidaResponseDTO cambiarUnidadMedida(Long productoId, UnidadMedidaRequestDTO dto);
    void eliminarProducto(Long id);
    List<ProductoConEstadoLoteDTO> buscarProductosConLotesPorEstado(String estado);
    List<ProductoConLotesDTO> buscarProductosConLotesAgrupadosPorEstado(String estado);
    Workbook generarReporteStockActualExcel();

    /**
     * Obtiene el producto como entidad para uso interno de otros módulos.
     *
     * @param id identificador del producto
     * @return entidad Producto encontrada
     * @throws IllegalArgumentException si no existe el producto
     */
    Producto findById(Long id);
}
