package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.mapper.TipoMovimientoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.*;
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
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @Transactional
    public MovimientoInventarioDTO registrarMovimiento(MovimientoInventarioDTO dto) {

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

        TipoMovimientoDetalle tipoMovimientoDetalle = tipoMovimientoDetalleRepository.findById(dto.tipoMovimientoDetalleId())
                .orElseThrow(() -> new NoSuchElementException("Tipo de movimiento detalle no encontrado"));

        MovimientoInventario movimiento = MovimientoInventarioMapper.toEntity(dto);
        movimiento.setProducto(producto);
        movimiento.setAlmacen(almacen);
        movimiento.setProveedor(proveedor);
        movimiento.setOrdenCompra(ordenCompra);
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle);

        MovimientoInventario entidadGuardada = repository.save(movimiento);
        return MovimientoInventarioMapper.toDTO(entidadGuardada);
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


