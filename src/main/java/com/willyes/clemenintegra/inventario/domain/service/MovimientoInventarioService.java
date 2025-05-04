package com.willyes.clemenintegra.inventario.domain.service;

import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.application.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.domain.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.domain.repository.MovimientoInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository repository;

    public MovimientoInventario registrarMovimiento(MovimientoInventarioDTO dto) {
        MovimientoInventario movimiento = MovimientoInventarioMapper.toEntity(dto);
        movimiento.setFechaIngreso(LocalDateTime.now());
        return repository.save(movimiento);
    }

    public Page<MovimientoInventario> consultarMovimientosConFiltros(MovimientoInventarioFiltroDTO filtro, Pageable pageable) {
        return repository.filtrarMovimientos(
                filtro.productoId(),
                filtro.almacenId(),
                filtro.tipoMovimiento(),
                filtro.fechaInicio(),
                filtro.fechaFin(),
                pageable
        );
    }

}

