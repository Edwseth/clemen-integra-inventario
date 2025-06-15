package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.CategoriaProductoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaProductoServiceImpl implements CategoriaProductoService {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaProductoMapper categoriaProductoMapper;

    public List<CategoriaProductoResponseDTO> listarTodas() {
        return categoriaProductoRepository.findAll()
                .stream()
                .map(categoriaProductoMapper::toDto)
                .collect(Collectors.toList());
    }

    public CategoriaProductoResponseDTO obtenerPorId(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return categoriaProductoMapper.toDto(categoria);
    }

    public CategoriaProductoResponseDTO crear(CategoriaProductoRequestDTO dto) {
        CategoriaProducto categoria = CategoriaProducto.builder()
                .nombre(dto.getNombre())
                .tipo(dto.getTipo())
                .build();
        categoriaProductoRepository.save(categoria);
        return categoriaProductoMapper.toDto(categoria);
    }

    public CategoriaProductoResponseDTO actualizar(Long id, CategoriaProductoRequestDTO dto) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        categoria.setNombre(dto.getNombre());
        categoria.setTipo(dto.getTipo());

        categoriaProductoRepository.save(categoria);
        return categoriaProductoMapper.toDto(categoria);
    }

    public void eliminar(Long id) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (productoRepository.existsByCategoriaProducto(categoria)) {
            throw new IllegalStateException("No se puede eliminar la categoría porque está en uso por productos.");
        }

        categoriaProductoRepository.delete(categoria);
    }

    // Mapeo delegado a MapStruct
}
