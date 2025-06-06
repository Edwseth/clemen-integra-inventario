package com.willyes.clemenintegra.inventario.application.service;

import com.willyes.clemenintegra.inventario.domain.model.Producto;
import com.willyes.clemenintegra.inventario.domain.repository.*;
import com.willyes.clemenintegra.inventario.domain.repository.UsuarioRepository;
import com.willyes.clemenintegra.inventario.application.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.ProductoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteProductoRepository loteProductoRepository;

    public List<ProductoResponseDTO> listarTodos() {
        return productoRepository.findAll()
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto) {
        validarDuplicados(dto.getCodigoSku(), dto.getNombre());

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        var usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Producto producto = Producto.builder()
                .codigoSku(dto.getCodigoSku())
                .nombre(dto.getNombre())
                .descripcionProducto(dto.getDescripcionProducto())
                .stockActual(dto.getStockActual())
                .stockMinimo(dto.getStockMinimo())
                .stockMinimoProveedor(dto.getStockMinimoProveedor())
                .activo(dto.getActivo())
                .requiereInspeccion(dto.getRequiereInspeccion())
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build();

        productoRepository.save(producto);
        return mapearADTO(producto);
    }

    private void validarDuplicados(String sku, String nombre) {
        if (productoRepository.existsByCodigoSku(sku)) {
            throw new IllegalArgumentException("Ya existe un producto con ese código SKU.");
        }
        if (productoRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }
    }

    private ProductoResponseDTO mapearADTO(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .codigoSku(producto.getCodigoSku())
                .nombre(producto.getNombre())
                .descripcionProducto(producto.getDescripcionProducto())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .activo(producto.getActivo())
                .requiereInspeccion(producto.getRequiereInspeccion())
                .unidadMedida(producto.getUnidadMedida().getNombre())
                .categoria(producto.getCategoriaProducto().getNombre())
                .fechaCreacion(producto.getFechaCreacion())
                .build();
    }

    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        return mapearADTO(producto);
    }

    public ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Validar duplicidad si se cambia el nombre o SKU
        if (!producto.getCodigoSku().equals(dto.getCodigoSku()) &&
                productoRepository.existsByCodigoSku(dto.getCodigoSku())) {
            throw new IllegalArgumentException("Ya existe otro producto con el mismo código SKU.");
        }

        if (!producto.getNombre().equals(dto.getNombre()) &&
                productoRepository.existsByNombre(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe otro producto con el mismo nombre.");
        }

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        var usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Actualizar campos
        producto.setCodigoSku(dto.getCodigoSku());
        producto.setNombre(dto.getNombre());
        producto.setDescripcionProducto(dto.getDescripcionProducto());
        producto.setStockActual(dto.getStockActual());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setStockMinimoProveedor(dto.getStockMinimoProveedor());
        producto.setActivo(dto.getActivo());
        producto.setRequiereInspeccion(dto.getRequiereInspeccion());
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);
        producto.setCreadoPor(usuario);

        productoRepository.save(producto);

        return mapearADTO(producto);
    }

    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Aquí debes validar relaciones activas (ejemplo con lotes)
        boolean tieneRelacion = loteProductoRepository.existsByProducto(producto);
        if (tieneRelacion) {
            throw new IllegalStateException("No se puede eliminar el producto porque tiene lotes registrados.");
        }

        productoRepository.delete(producto);
    }
}

