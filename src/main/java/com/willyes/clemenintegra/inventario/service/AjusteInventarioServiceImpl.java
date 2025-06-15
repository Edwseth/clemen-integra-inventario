package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.AjusteInventarioMapper;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AjusteInventarioServiceImpl implements AjusteInventarioService {

    private final AjusteInventarioRepository repository;
    private final AjusteInventarioMapper mapper;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final UsuarioRepository usuarioRepository;


    public List<AjusteInventarioResponseDTO> listar() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public AjusteInventarioResponseDTO crear(AjusteInventarioRequestDTO dto) {

        if (dto.getCantidad().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero");
        }

        var producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        var almacen = almacenRepository.findById(dto.getAlmacenId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));
        var usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        var entity = mapper.toEntity(dto, producto, almacen, usuario);
        var guardado = repository.save(entity);
        return mapper.toResponseDTO(guardado);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

