package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
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
    private final LoteProductoRepository loteProductoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @Transactional
    public MovimientoInventarioDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        // 1) Cargar entidades base
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
        Almacen almacen = almacenRepository.findById(dto.almacenId())
                .orElseThrow(() -> new NoSuchElementException("Almacén no encontrado"));
        Proveedor proveedor = dto.proveedorId() != null
                ? proveedorRepository.findById(dto.proveedorId())
                .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado"))
                : null;
        OrdenCompra ordenCompra = dto.ordenCompraId() != null
                ? ordenCompraRepository.findById(dto.ordenCompraId())
                .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada"))
                : null;
        MotivoMovimiento motivo = dto.motivoMovimientoId() != null
                ? motivoMovimientoRepository.findById(dto.motivoMovimientoId())
                .orElseThrow(() -> new NoSuchElementException("Motivo de movimiento no encontrado"))
                : null;

        // 2) Cargar y validar Lote
        LoteProducto lote = loteProductoRepository.findById(dto.loteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

        // 3) Cargar Detalle y validar consistencia
        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository
                .findById(dto.tipoMovimientoDetalleId())
                .orElseThrow(() -> new NoSuchElementException("Detalle de movimiento no encontrado"));

        String desc = detalle.getDescripcion();
        ClasificacionMovimientoInventario clasifDetalle =
                ClasificacionMovimientoInventario.valueOf(desc);

        TipoMovimiento tipoEsperado =
                TipoMovimientoMapper.obtenerTipoMovimiento(clasifDetalle);

        TipoMovimiento tipoDto =
                TipoMovimiento.valueOf(dto.tipoMovimiento().name());

        if (!tipoEsperado.equals(tipoDto)) {
            throw new IllegalArgumentException(
                    "El tipo de movimiento no corresponde con el detalle especificado"
            );
        }

        // 4) Construir la entidad y asignar **todos** los campos relacionales
        MovimientoInventario mv = MovimientoInventarioMapper.toEntity(dto);
        mv.setProducto(producto);
        mv.setAlmacen(almacen);
        mv.setProveedor(proveedor);
        mv.setOrdenCompra(ordenCompra);
        mv.setMotivoMovimiento(motivo);
        mv.setLote(lote);
        mv.setTipoMovimientoDetalle(detalle);

        // 5) Guardar y convertir a DTO
        MovimientoInventario guardado = repository.save(mv);
        return MovimientoInventarioMapper.toDTO(guardado);
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


