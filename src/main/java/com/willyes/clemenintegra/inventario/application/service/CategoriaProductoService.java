package com.willyes.clemenintegra.inventario.application.service;

import com.willyes.clemenintegra.inventario.domain.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.domain.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.domain.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.application.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.CategoriaProductoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaProductoService {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProductoRepository productoRepository;

    public List<CategoriaProductoResponseDTO> listarTodas() {
        return categoriaProductoRepository.findAll()
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    public CategoriaProductoResponseDTO obtenerPorId(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return mapearADTO(categoria);
    }

    public CategoriaProductoResponseDTO crear(CategoriaProductoRequestDTO dto) {
        CategoriaProducto categoria = CategoriaProducto.builder()
                .nombre(dto.getNombre())
                .tipo(dto.getTipo())
                .build();
        categoriaProductoRepository.save(categoria);
        return mapearADTO(categoria);
    }

    public CategoriaProductoResponseDTO actualizar(Long id, CategoriaProductoRequestDTO dto) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        categoria.setNombre(dto.getNombre());
        categoria.setTipo(dto.getTipo());

        categoriaProductoRepository.save(categoria);
        return mapearADTO(categoria);
    }

    public void eliminar(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (productoRepository.existsByCategoriaProducto(categoria)) {
            throw new IllegalStateException("No se puede eliminar la categoría porque está en uso por productos.");
        }

        categoriaProductoRepository.delete(categoria);
    }

    private CategoriaProductoResponseDTO mapearADTO(CategoriaProducto categoria) {
        return CategoriaProductoResponseDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .tipo(categoria.getTipo())
                .build();
    }
}
