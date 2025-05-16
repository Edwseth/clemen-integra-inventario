package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;
import com.willyes.clemenintegra.inventario.mapper.TipoMovimientoDetalleMapper;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipoMovimientoDetalleService {

    private final TipoMovimientoDetalleRepository repository;
    private final TipoMovimientoDetalleMapper mapper;

    public List<TipoMovimientoDetalleDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public TipoMovimientoDetalleDTO crear(TipoMovimientoDetalleDTO dto) {
        TipoMovimientoDetalle entity = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(entity));
    }

    public void eliminarPorId(Long id) {
        repository.deleteById(id);
    }
}
