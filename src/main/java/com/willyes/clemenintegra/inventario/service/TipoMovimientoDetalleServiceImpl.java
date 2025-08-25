package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;
import com.willyes.clemenintegra.inventario.mapper.TipoMovimientoDetalleMapper;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TipoMovimientoDetalleServiceImpl implements TipoMovimientoDetalleService {

    private final TipoMovimientoDetalleRepository repository;
    private final TipoMovimientoDetalleMapper mapper;

    public Page<TipoMovimientoDetalleDTO> listarTodos(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDTO);
    }

    public TipoMovimientoDetalleDTO crear(TipoMovimientoDetalleDTO dto) {
        TipoMovimientoDetalle entity = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(entity));
    }

    public void eliminarPorId(Long id) {
        repository.deleteById(id);
    }
}
