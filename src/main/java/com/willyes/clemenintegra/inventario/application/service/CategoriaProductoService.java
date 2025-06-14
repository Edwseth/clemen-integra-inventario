package com.willyes.clemenintegra.inventario.application.service;

import com.willyes.clemenintegra.inventario.application.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.application.mapper.CategoriaProductoMapper;
import com.willyes.clemenintegra.inventario.domain.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.domain.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaProductoService {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaProductoMapper mapper;

    public List<CategoriaProductoResponseDTO> listarTodas() {
        return categoriaProductoRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public CategoriaProductoResponseDTO obtenerPorId(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return mapper.toDTO(categoria);
    }

    public CategoriaProductoResponseDTO crear(CategoriaProductoRequestDTO dto) {
        CategoriaProducto categoria = mapper.toEntity(dto);
        categoriaProductoRepository.save(categoria);
        return mapper.toDTO(categoria);
    }

    public CategoriaProductoResponseDTO actualizar(Long id, CategoriaProductoRequestDTO dto) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        mapper.updateEntityFromDto(dto, categoria);

        categoriaProductoRepository.save(categoria);
        return mapper.toDTO(categoria);
    }

    public void eliminar(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (productoRepository.existsByCategoriaProducto(categoria)) {
            throw new IllegalStateException("No se puede eliminar la categoría porque está en uso por productos.");
        }

        categoriaProductoRepository.delete(categoria);
    }
}
