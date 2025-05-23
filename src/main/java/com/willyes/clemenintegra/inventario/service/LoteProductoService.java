package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoteProductoService {

    private final LoteProductoRepository loteRepo;
    private final ProductoRepository productoRepo;
    private final AlmacenRepository almacenRepo;

    public LoteProductoService(LoteProductoRepository loteRepo, ProductoRepository productoRepo, AlmacenRepository almacenRepo) {
        this.loteRepo = loteRepo;
        this.productoRepo = productoRepo;
        this.almacenRepo = almacenRepo;
    }

    @Transactional
    public LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto) {
        Producto producto = productoRepo.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Almacen almacen = almacenRepo.findById(dto.getAlmacenId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        LoteProducto lote = LoteProductoMapper.toEntity(dto, producto, almacen);
        lote = loteRepo.save(lote);
        return LoteProductoMapper.toDto(lote);
    }

    public List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado) {
        EstadoLote estadoEnum = EstadoLote.valueOf(estado.toUpperCase());
        return loteRepo.findByEstado(estadoEnum).stream()
                .map(LoteProductoMapper::toDto)
                .collect(Collectors.toList());
    }
}

