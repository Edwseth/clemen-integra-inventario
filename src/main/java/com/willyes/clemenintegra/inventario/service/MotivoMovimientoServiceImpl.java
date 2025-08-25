package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MotivoMovimientoMapper;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MotivoMovimientoServiceImpl implements MotivoMovimientoService {

    private final MotivoMovimientoRepository repository;
    private final MotivoMovimientoMapper mapper;

    @Override
    public Page<MotivoMovimientoResponseDTO> listar(Pageable pageable) {
        Page<MotivoMovimiento> page = repository.findAll(pageable);
        return page.map(mapper::toDTO);
    }
}