package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioDTO;
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

    public List<AjusteInventarioDTO> listar() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public AjusteInventarioDTO crear(AjusteInventarioDTO dto) {
        var entity = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(entity));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

