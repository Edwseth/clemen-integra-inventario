package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.mapper.BitacoraCambiosInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.BitacoraCambiosInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BitacoraCambiosInventarioService {

    private final BitacoraCambiosInventarioRepository repository;
    private final BitacoraCambiosInventarioMapper mapper;

    public List<BitacoraCambiosInventarioDTO> listar() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public BitacoraCambiosInventarioDTO crear(BitacoraCambiosInventarioDTO dto) {
        var entity = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(entity));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

