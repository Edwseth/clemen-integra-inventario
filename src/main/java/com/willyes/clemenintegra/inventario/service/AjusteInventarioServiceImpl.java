package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.AjusteInventarioMapper;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AjusteInventarioServiceImpl implements AjusteInventarioService {

    private final AjusteInventarioRepository repository;
    private final AjusteInventarioMapper mapper;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final UsuarioRepository usuarioRepository;


    public Page<AjusteInventarioResponseDTO> listar(Pageable pageable) {
        Page<AjusteInventario> page = repository.findAll(pageable);
        return page.map(mapper::toResponseDTO);
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
        entity.setFecha(LocalDateTime.now());
        var guardado = repository.save(entity);
        return mapper.toResponseDTO(guardado);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

