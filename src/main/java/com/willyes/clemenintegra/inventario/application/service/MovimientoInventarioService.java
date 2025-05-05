package com.willyes.clemenintegra.inventario.application.service;

import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.application.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.domain.model.*;
import com.willyes.clemenintegra.inventario.domain.repository.*;
import com.willyes.clemenintegra.inventario.application.mapper.TipoMovimientoMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository repository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;

    @Transactional
    public MovimientoInventario registrarMovimiento(MovimientoInventarioDTO dto) {

        // Validar existencia de entidades
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        Almacen almacen = almacenRepository.findById(dto.almacenId())
                .orElseThrow(() -> new NoSuchElementException("Almacén no encontrado"));

        Proveedor proveedor = dto.proveedorId() != null ?
                proveedorRepository.findById(dto.proveedorId())
                        .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado")) : null;

        OrdenCompra ordenCompra = dto.ordenCompraId() != null ?
                ordenCompraRepository.findById(dto.ordenCompraId())
                        .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada")) : null;

        MotivoMovimiento motivo = dto.motivoMovimientoId() != null ?
                motivoMovimientoRepository.findById(dto.motivoMovimientoId())
                        .orElseThrow(() -> new NoSuchElementException("Motivo de movimiento no encontrado")) : null;

        // Validar coherencia entre tipoMovimientoDetalle y tipoMovimiento
        TipoMovimiento tipoEsperado = TipoMovimientoMapper.obtenerTipoMovimiento(dto.tipoMovimientoDetalle());
        if (!tipoEsperado.equals(dto.tipoMovimiento())) {
            throw new IllegalArgumentException("El tipo de movimiento no corresponde con el detalle especificado");
        }

        MovimientoInventario movimiento = MovimientoInventarioMapper.toEntity(dto);
        movimiento.setProducto(producto);
        movimiento.setAlmacen(almacen);
        movimiento.setProveedor(proveedor);
        movimiento.setOrdenCompra(ordenCompra);
        movimiento.setMotivoMovimiento(motivo);

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


