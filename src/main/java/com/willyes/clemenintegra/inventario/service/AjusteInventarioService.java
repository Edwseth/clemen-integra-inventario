package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.AjusteInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.AjusteInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AjusteInventarioService {

    private final AjusteInventarioRepository repository;
    private final AjusteInventarioMapper mapper;

    public List<AjusteInventarioResponseDTO> listar() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public AjusteInventarioResponseDTO crear(AjusteInventarioRequestDTO dto) {
        var entity = mapper.toEntity(dto);
        var guardado = repository.save(entity);
        return mapper.toResponseDTO(guardado);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

